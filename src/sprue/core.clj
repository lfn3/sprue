(ns sprue.core
  (:require [sprue.kotlin-output :as kotlin]
            [sprue.specs :as specs]
            [sprue.java-output :as java])
  (:import (java.io StringWriter OutputStreamWriter))
  (:gen-class))

(defn id-name [class-name] (str class-name "Id"))

(defn id-for [{:keys [name package] :as class-config}]
  {:name             (id-name name)
   ::specs/generator :id
   :package          package})

(defn with-id-classes [classes] (concat classes (map id-for classes)))

(defn ided-class-for [{:keys [name package] :as class-config}]
  {:name             (str "Ided" name)
   ::specs/generator :data
   :fields           (cons {:name "id"
                  :type [package (id-name name)]}
                 (get class-config :fields []))})

(defn merge-all-template [templates]
  (->> specs/generator-types
       (map (fn [k] [k (merge (:all templates) (get templates k))]))
       (into {})))

(defn merge-templates [templates class] (merge class (get templates (::specs/generator class))))

(def package "net.lfn3.sprue")

(def sample-templates (merge-all-template {:all {:package package}
                                           :id  {:extends ["net.lfn3.sprue" "Id"]}}))

(def sample-class (merge-templates sample-templates
                                   {:name             "Test"
                                    ::specs/generator :data
                                    :fields           [{:name        "name"
                                              :type        String
                                              :annotations [{:package package
                                                             :name    "AnAnnotation"
                                                             :members [["value = %S" "1"]]}]}]}))

(def sample-classes (->> [sample-class]
                         (with-id-classes)
                         (concat [(ided-class-for sample-class)])
                         (map (partial merge-templates sample-templates))))

(defn string-writer-for [file-suffix]
  (fn [package filename]
    (doto (StringWriter.)
      (.write (str "//" package \. filename file-suffix \newline)))))

(def file-suffixes
  {"kotlin" ".kt"
   "java" ".java"})

(def emitters
  {"kotlin" kotlin/emit-classes
   "java" java/emit-classes})

(defn -main
  "I don't do a whole lot ... yet."
  [& [language]]
  (if-let [emit-fn (get emitters language)]
    (->> (emit-fn sample-classes (string-writer-for (get file-suffixes language)) str)
         (map println)
         (dorun))
    (do (println "Expected either `java` or `kotlin` as first argument, got" language)
        (System/exit 1))))
