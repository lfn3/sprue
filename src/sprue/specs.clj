(ns sprue.specs
  (:require [clojure.spec.alpha :as s])
  (:import (java.lang.reflect Type)))

(def generator-types #{:id :data})

(s/def ::generator #{:id :data})

(s/def ::type (s/or :class (partial instance? Type)
                    :package-and-classname (s/cat :package string? :classname string?)))

(s/def ::extends ::type)