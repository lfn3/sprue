(ns sprue.repl
  (:require [camel-snake-kebab.core :as csk]
            [sprue.java-output :as jo]
            [sprue.input.core]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s])
  (:import (java.time LocalDate)
           (java.io StringWriter FileWriter Writer)
           (com.squareup.javapoet JavaFile)))

; Define these yourself once you've loaded this ns
(defonce use-namespace "")
(defonce id-base-type ["" ""])
(defonce api-id-prefix "")
(defonce api-id-suffix "")

(def id-field-name "id")
(def id-type-suffix "Id")

(defn id-type-name [name] (str name id-type-suffix))

(def log-id-field-name "logId")
(defn log-id-type-name [name] (str name "Log" id-type-suffix))

(defn const-field-name [name] (csk/->SCREAMING_SNAKE_CASE name))

(defn id-field-name-const-name [name]
  (const-field-name (str (id-type-name name) "SerializerFieldName")))

(defn log-id-field-name-const-name [name]
  (const-field-name (str (log-id-type-name name) "SerializerFieldName")))

(defn id-api-name* [name prefix suffix] (str prefix (csk/->snake_case name) suffix))
(defn id-api-name [name] (id-api-name* name api-id-prefix api-id-suffix))
(defn log-id-api-name* [name prefix suffix] (id-api-name* name prefix (str "_log" suffix)))
(defn log-id-api-name [name] (str api-id-prefix (csk/->snake_case name) "_log" api-id-suffix))

(defn id-type* [name namespace id-base-type]
  (-> {:name      (id-type-name name)
       :namespace namespace
       :flags     [{:name              :base-type
                    :base-type         id-base-type
                    :super-ctor-fields #{id-field-name}}]
       :fields    [{:name id-field-name
                    :type Long/TYPE}]}
      (sprue.input.core/normalize-type)))

(defn id-type [name] (id-type* name use-namespace id-base-type))

(defn id-field-flags [type-name const-field-name]
  ;TODO: rip out assumptions around these values
  ;TODO: also probably shouldn't bother emitting these (or the constants) if the suffixes and prefixes are blank.
  [{:name :serialize-field-name-as :value const-field-name}
   {:name :serialize-field-value-as :value Long/TYPE}])

(defn entity-type* [name fields namespace api-id-prefix api-id-suffix]
  (-> {:name      name
       :namespace namespace
       :fields    (concat (->> [{:name  id-field-name
                                 :type  [namespace (id-type-name name)]
                                 :flags (id-field-flags name (id-field-name-const-name name))}
                                {:name  log-id-field-name
                                 :type  [namespace (log-id-type-name name)]
                                 :flags (id-field-flags name (log-id-field-name-const-name name))}
                                {:name  (id-field-name-const-name name)
                                 :type  String
                                 :flags [:const {:name  :initializer
                                                 :value (id-api-name* name api-id-prefix api-id-suffix)}]}
                                {:name  (log-id-field-name-const-name name)
                                 :type  String
                                 :flags [:const {:name  :initializer
                                                 :value (log-id-api-name* name api-id-prefix api-id-suffix)}]}
                                {:name "effectiveDate"
                                 :type LocalDate}
                                {:name "inEffect"
                                 :type Boolean/TYPE}
                                {:name "username"
                                 :type String}]
                               (map #(update %1 :flags conj :final)))
                          fields)}
      (sprue.input.core/normalize-type)))

(defn entity-type [name fields] (entity-type* name fields use-namespace api-id-prefix api-id-suffix))

(defn ^JavaFile to-java-file [normalized-type-spec] (jo/render-to-javafile normalized-type-spec))

(defn string-writer-for [package filename & [suppress-comment?]]
  (let [file-suffix ".java"
        filename-comment (if (str/ends-with? package filename)
                           (str "//" package file-suffix \newline)
                           (str "//" package \. filename file-suffix \newline))
        sw (StringWriter.)]
    (when-not suppress-comment?
      (.write sw filename-comment))
    sw))

(defn show-generated [{:keys [namespace name] :as type-spec}]
  (let [^JavaFile jf (to-java-file type-spec)]
    (with-open [^Writer sw (string-writer-for namespace name true)]
      (.writeTo jf sw)
      (str sw))))

(defn barf-generated
  ([not-yet-normalized-type-spec path]
   (let [^JavaFile jf (to-java-file not-yet-normalized-type-spec)]
     (with-open [^Writer fw (FileWriter. (io/file path))]
       (.writeTo jf fw)
       (.flush fw))))
  ([not-yet-normalized-type-spec base-dir package filename]
   (barf-generated not-yet-normalized-type-spec (apply io/file base-dir (conj (str/split package #"\.") filename)))))

(defn derive-type [normalized-type name field-names-to-exclude]
  (-> normalized-type
      (assoc :name name)
      (update :fields (partial filter #(not (jo/find-optional-flag (:flags %1) :const))))
      (update :fields (partial filter #(not (field-names-to-exclude (:name %1)))))))

(s/fdef derive-type
        :args (s/cat :normalized-type :sprue.input.normalized/type
                     :name string?
                     :field-names-to-exclude (s/and set? (s/coll-of string?)))
        :ret :sprue.input.normalized/type)