(ns sprue.java-output
  (:require [sprue.java-interop-util :as jiu]
            [sprue.specs :as specs]
            [clojure.string :as str])
  (:import (com.squareup.javapoet JavaFile TypeSpec MethodSpec AnnotationSpec ClassName TypeSpec$Builder ParameterSpec MethodSpec$Builder FieldSpec)
           (java.io Writer)
           (javax.lang.model.element Modifier)))

(defn ^ClassName poet-class-name [package name & more-name-parts]
  (ClassName/get package name (jiu/coll-str-arr more-name-parts)))

(defn convert-type [type]
  (if (vector? type)
    (let [[package class] type]
      (poet-class-name package class))
    type))

(defn ^AnnotationSpec generated-annotation []
  (-> (poet-class-name "javax.annotation.processing" "Generated")
      (AnnotationSpec/builder)
      (.build)))

(defn ^ParameterSpec parameter-spec [{:keys [^String name type annotations] :as field}]
  (-> (convert-type type)
      (ParameterSpec/builder name (into-array Modifier []))
      (.build)))

(defn add-parameter [^MethodSpec$Builder method-builder parameter-spec]
  (.addParameter method-builder parameter-spec))

(defn assignment [{:keys [name] :as field}]
  (str "this." name " = " name \; \newline))

(defn data-ctor-body [fields]
  (->> fields
       (map assignment)
       (str/join)))

(defn add-parameters [method-builder fields]
  (->> fields
       (map parameter-spec)
       (map (partial add-parameter method-builder))
       (dorun))
  method-builder)

(defn ctor-builder [fields]
  (-> (MethodSpec/constructorBuilder)
      (.addModifiers (into-array [Modifier/PUBLIC]))
      (add-parameters fields)))

(defn ^MethodSpec build-ctor [fields]
  (-> (ctor-builder fields)
      (.addCode (data-ctor-body fields) (jiu/str-arr))
      (.build)))

(defn ^TypeSpec$Builder class-builder [{:keys [^String name fields] :as class-config}]
  (doto (TypeSpec/classBuilder name)
    (.addAnnotation (generated-annotation))))

(defn add-member [builder [name format-str & format-args]]
  (.addMember builder name (str \$ format-str) (jiu/coll-str-arr format-args)))

(defn annotation-spec [{:keys [type members]}]
  (as-> (AnnotationSpec/builder (convert-type type)) builder
        (reduce add-member builder members)
        (.build builder)))

(defn add-annotation [builder annotation] (-> builder (.addAnnotation (annotation-spec annotation))))

(defn add-annotations [builder annotations] (reduce add-annotation builder annotations))

(defn ^FieldSpec field-spec [{:keys [name type annotations] :as field}]
  (-> (convert-type type)
      (FieldSpec/builder name (into-array Modifier [Modifier/FINAL Modifier/PRIVATE]))
      (add-annotations annotations)
      (.build)))

(defn add-field [class-builder field]
  (.addField class-builder (field-spec field))
  class-builder)

(defn add-fields [class-builder fields]
  (->> fields
       (map (partial add-field class-builder))
       (dorun))
  class-builder)

(def equals-preamble
  (str "if (this == o) return true;\n"
       "if (o == null || getClass() != o.getClass()) return false;\n"))

(defn add-equals-lines [builder fields]
  (let [code-str (str (->> fields
                       (map :name)
                       (map #(str "Objects.equals(this." %1 ", that." %1 ")"))
                       (str/join " &&\n\t"))
                      ";\n")]
    (.addCode builder code-str (jiu/str-arr))))

(defn add-equals-method [class-builder type fields]
  ;TODO could be improved if we know a field is a) nullable and b) primitive
  (let [method-spec (-> (MethodSpec/methodBuilder "equals")
                        (.returns Boolean/TYPE)
                        (.addParameter Object "o" (into-array [Modifier/PUBLIC]))
                        (.addCode equals-preamble (jiu/str-arr))
                        (.addCode "%T that = (%T) o;\n" (into-array [(convert-type type) (convert-type type)]))
                        (.addCode "return " (jiu/str-arr))  ;TODO!
                        (add-equals-lines fields)
                        (.build))]
    (.addMethod class-builder method-spec))
  class-builder)

(defn add-hashcode-method [class-builder fields]
  (let [methodSpec
        (-> (MethodSpec/methodBuilder "hashcode")
            (.returns Integer/TYPE)
            (.addCode (str "Objects.hash(" (str/join ", " (map :name fields)) ");\n") (jiu/str-arr))
            (.build))]
    (.addMethod class-builder methodSpec)))

(defn add-to-string-method [class-builder fields]
  ;TODO
  class-builder)

(defn getter-for [{:keys [name type] :as field}]
  (-> (str "get" (Character/toUpperCase (first name)) (.substring name 1))
      (MethodSpec/methodBuilder)
      (.addModifiers (into-array [Modifier/PUBLIC]))
      (.returns (convert-type type))
      (.addCode (str "return this." name ";\n") (jiu/str-arr))
      (.build)))

(defn add-getter-method [class-builder field]
  (.addMethod class-builder (getter-for field)))

(defn add-getter-methods [class-builder fields]
  (->> fields
       (map (partial add-getter-method class-builder))
       (dorun))
  class-builder)


(defn make-data-class [{:keys [fields name package] :as class-config}]
  (-> class-config
      (class-builder)
      (.addMethod (build-ctor fields))
      (add-fields fields)
      (add-equals-method [package name] fields)
      (add-hashcode-method fields)
      (add-to-string-method fields)
      (add-getter-methods fields)
      (.build)))

(defn make-id-ctor [fields]
  (-> (ctor-builder fields)
      (.addCode (str "super(" (str/join ", " (map :name fields)) ");\n") (jiu/str-arr))
      (.build)))

(defn make-id-class [{:keys [extends] :as class-config}]
  (let [fields [{:name "id" :type Long/TYPE}]]
    (-> (assoc class-config :fields fields)
        (class-builder)
        (.superclass (convert-type extends))
        (.addMethod (make-id-ctor fields))
        (.build))))

(defmulti make-class ::specs/generator)
(defmethod make-class :data [class-config] (make-data-class class-config))
(defmethod make-class :id [class-config] (make-id-class class-config))

(defn ^JavaFile wrap-in-file [{:keys [package name] :as class-config}]
  (.build (JavaFile/builder (str package \. name) (make-class class-config))))


(defn emit-classes [classes writer-fn & [before-close]]
  (let [writer-fn-adapter (fn [^JavaFile jf]
                            (with-open [^Writer writer (writer-fn (.-packageName jf) (.-name (.-typeSpec jf)))]
                              (.writeTo jf writer)
                              (when before-close
                                (before-close writer))))]
    (->> classes
         (map wrap-in-file)
         (map writer-fn-adapter)
         (doall))))