(ns sprue.specs
  (:require [clojure.spec.alpha :as s])
  (:import (java.lang.reflect Type)))

(def generator-types #{:id :data})

(s/def ::generator generator-types)

(s/def ::type (s/or :class (partial instance? Type)
                    :package-and-classname (s/cat :package string? :classname string?)))

(s/def ::extends ::type)

(s/def ::annotation-member-name string?)
(s/def ::format-str string?)
(s/def ::format-vals (s/coll-of any?))

(s/def ::member (s/tuple ::annotation-member-name ::format-str ::format-vals))
(s/def ::members (s/coll-of ::member))

(s/def ::location #{::field ::getter ::param})
(s/def ::locations (s/coll-of ::location))

(s/def ::annotation (s/keys :req [::type] :opt [::members ::locations]))

(s/def ::annotations (s/coll-of ::annotation))

(s/def ::name string?)

(s/def ::field (s/keys :req [::name ::type] :opt [::annotations]))

(s/def ::package string?)
(s/def ::extends ::type)
(s/def ::class (s/keys :req [::name ::generator ::package] :opt [::extends]))
(s/def ::class-opt (s/keys :opt [::package ::extends]))

(s/def ::classes (s/coll-of ::class))

(def template-keys (conj generator-types ::all))
(s/def ::template-keys template-keys)

(s/def ::templates (s/map-of template-keys ::class-opt))

(s/def ::nullable-annotation ::type)
(s/def ::not-nullable-annotation ::type)

(s/def ::configuration (s/keys :req [::classes] :opt [::templates ::nullable-annotation ::not-nullable-annotation]))