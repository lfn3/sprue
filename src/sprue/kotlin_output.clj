(ns sprue.kotlin-output
  (:require [sprue.java-interop-util :as jiu]
            [sprue.specs :as specs])
  (:import (com.squareup.kotlinpoet TypeSpec FileSpec KModifier FunSpec FunSpec$Builder TypeSpec$Builder PropertySpec ClassName AnnotationSpec)
           (java.io Writer)))

(defn k-mods [& modifiers] (into-array KModifier modifiers))

(defn ^ClassName poet-class-name [^String package ^String name & more-name-parts]
  (ClassName. package name (jiu/coll-str-arr more-name-parts)))

(defn convert-type [type]
  (if (vector? type)
    (let [[package class] type]
      (poet-class-name package class))
    type))

(defn add-ctor-param [^FunSpec$Builder ctor-builder {:keys [^String name type] :as field}]
  (.addParameter ctor-builder name (convert-type type) (k-mods)))

(defn build-ctor [fields]
  (let [builder (FunSpec/constructorBuilder)]
    (->> fields (map (partial add-ctor-param builder)) (dorun))
    (.build builder)))

(defn add-member [builder [name format-str & format-args]]
  (.addMember builder (str name " = %" format-str) (jiu/coll-str-arr format-args)))

(defn annotation-spec [{:keys [type members]}]
  (as-> (AnnotationSpec/builder (convert-type type)) builder
        (reduce add-member builder members)
        (.build builder)))

(defn add-annotation [builder annotation] (-> builder (.addAnnotation (annotation-spec annotation))))

(defn add-annotations [builder annotations] (reduce add-annotation builder annotations))

(defn prop-spec [{:keys [^String name type annotations] :as field}]
  (-> (PropertySpec/builder name (convert-type type) (k-mods))
      (.initializer name (into-array []))
      (add-annotations annotations)
      (.build)))

(defn add-property [^TypeSpec$Builder type-builder field]
  (.addProperty type-builder (prop-spec field)))

(def generator-name "net.lfn3.sprue.Core")

(defn ^AnnotationSpec generated-annotation []
  (-> (AnnotationSpec/builder (poet-class-name "javax.annotation.processing" "Generated"))
      (.addMember "%S" (jiu/str-arr generator-name))
      (.build)))

(defn class-builder [{:keys [^String name fields] :as class-config}]
  (doto (TypeSpec/classBuilder name)
    (.primaryConstructor (build-ctor fields))
    (.addAnnotation (generated-annotation))))

(defn make-data-class [{:keys [^String name fields] :as class-config}]
  (let [builder (doto (class-builder class-config)
                  (.addModifiers (k-mods KModifier/DATA)))]
    (->> fields
         (map (partial add-property builder))
         (dorun))
    (.build builder)))

(defn make-id-class [{:keys [^String name extends] :as class-config}]
  (let [fields [{:name "id"
                 :type Long/TYPE}]
        builder (doto (class-builder (assoc class-config :fields fields))
                  (.superclass (convert-type extends))
                  (.addSuperclassConstructorParameter "id" (into-array Object [])))]
    (.build builder)))

(defmulti make-class ::specs/generator)
(defmethod make-class :data [class-config] (make-data-class class-config))
(defmethod make-class :id [class-config] (make-id-class class-config))

(defn ^FileSpec wrap-in-file [{:keys [package name] :as class-config}]
  (.build (doto (FileSpec/builder package name)
            (.addType (make-class class-config)))))


(defn emit-classes [classes writer-fn & [before-close]]
  (let [writer-fn-adapter (fn [^FileSpec fs]
                            (with-open [^Writer writer (writer-fn (.getPackageName fs) (.getName fs))]
                              (.writeTo fs writer)
                              (when before-close
                                (before-close writer))))]
   (->> classes
        (map wrap-in-file)
        (map writer-fn-adapter)
        (doall))))