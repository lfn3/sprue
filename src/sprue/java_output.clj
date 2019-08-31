(ns sprue.java-output
  (:require [sprue.java-interop-util :as jiu])
  (:import (com.squareup.javapoet JavaFile TypeSpec MethodSpec AnnotationSpec ClassName TypeSpec$Builder)
           (java.io Writer)))

(defn ^ClassName poet-class-name [package name & more-name-parts]
  (ClassName/get package name (jiu/coll-str-arr more-name-parts)))

(defn ^AnnotationSpec generated-annotation []
  (-> (poet-class-name "javax.annotation.processing" "Generated")
      (AnnotationSpec/builder)
      (.build)))

(defn ^MethodSpec build-ctor [fields]
  (let [builder (MethodSpec/constructorBuilder)]
    (.build builder)))

(defn ^TypeSpec$Builder class-builder [{:keys [^String name fields] :as class-config}]
  (doto (TypeSpec/classBuilder name)
    (.addMethod (build-ctor fields))
    (.addAnnotation (generated-annotation))))

(defn make-data-class [{:as class-config}]
  (-> class-config
      (class-builder)
      (.build)))

(defn make-id-class [{:as class-config}]
  (-> class-config
      (class-builder)
      (.build)))

(defmulti make-class :type)
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