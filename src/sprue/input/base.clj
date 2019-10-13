(ns sprue.input.base
  (:require [clojure.spec.alpha :as s]))

(s/def ::name string?)

(s/def ::namespace-and-type-name (s/tuple string? string?))

(s/def ::type (s/or :class class?
                    :namespace-and-type-name ::namespace-and-type-name))