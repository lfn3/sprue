(ns sprue.core
  (:require [sprue.kotlin-output :as kotlin]
            [sprue.java-output :as java])
  (:import (java.io StringWriter OutputStreamWriter))
  (:gen-class))

(defn id-name [class-name] (str class-name "Id"))

(defn id-for [{:keys [name package] :as class-config}]
  {:name    (id-name name)
   :type    :id
   :package package})

(defn with-id-classes [classes] (concat classes (map id-for classes)))

(defn ided-class-for [{:keys [name package] :as class-config}]
  {:name   (str "Ided" name)
   :type   :data
   :fields (cons {:name "id"
                  :type (java/poet-class-name package (id-name name))}
                 (get class-config :fields []))})

(def type-keys #{:id :data})

(defn merge-all-template [templates]
  (->> type-keys
       (map (fn [k] [k (merge (:all templates) (get templates k))]))
       (into {})))

(defn merge-templates [templates class] (merge class (get templates (:type class))))

(def package "net.lfn3.sprue")

(def sample-templates (merge-all-template {:all {:package package}
                                           :id  {:extends ["net.lfn3.sprue" "Id"]}}))

(def sample-class (merge-templates sample-templates
                                   {:name   "Test"
                                    :type   :data
                                    :fields [{:name        "name"
                                              :type        String
                                              :annotations [{:package package
                                                             :name    "AnAnnotation"
                                                             :members [["value = %S" "1"]]}]}]}))

(def sample-classes (->> [sample-class]
                         (with-id-classes)
                         (concat [(ided-class-for sample-class)])
                         (map (partial merge-templates sample-templates))))

(defn string-writer-for [package filename]
  (doto (StringWriter.)
    (.write (str "//" package \. filename ".kt\n"))))

(defn sout-writer-for [package filename]
  (doto (OutputStreamWriter. System/out)
    (.write (str "//" package \. filename ".kt\n"))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (->> (java/emit-classes sample-classes string-writer-for str)
       (map println)
       (dorun)))
