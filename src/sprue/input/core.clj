(ns sprue.input.core
  (:require [clojure.spec.alpha :as s]
            [sprue.input.type :as type]
            [sprue.input.normalized :as normalized]))

(s/def ::defaults (s/keys :opt-un [::type/namespace]))
(s/def ::output-dir string?)

(s/def ::input (s/keys :req-un [::type/types] :opt-un [::defaults]))

(defn normalize-flag [flag]
  (if (keyword? flag)
    {:name flag}
    flag))

(defn expand-normalizable-flags [type]
  )

(s/fdef expand-normalizable-flags
        :args (s/cat :type ::normalized/type)
        :ret ::normalized/types)                            ;One type can expand to multiple others given some flags

(defn normalize-type-flags [type]
  (update type :flags (partial map normalize-flag)))

(defn normalize-field-flags [type]
  (update type :fields (partial map #(update %1 :flags (partial map normalize-flag)))))

(defn normalize-type [type]
  (-> type (normalize-type-flags) (normalize-field-flags)))

(defn normalize [{:keys [types defaults]}]
  (->> types
       (map (partial merge defaults))
       (map normalize-type)))

(s/fdef normalize
        :args (s/cat :input ::input)
        :ret ::normalized/types)

(defn to-java-ir [input]
  (->> input
       (normalize)))

(s/fdef to-java-ir :args (s/cat :input ::input))