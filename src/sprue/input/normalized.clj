(ns sprue.input.normalized
  (:require [clojure.spec.alpha :as s]
            [sprue.input.base :as base]
            [sprue.input.type :as type]))

(s/def ::name ::type/simple-flag)
(s/def ::simple-flag (s/keys :req-un [::name]))
(s/def ::flag (s/or :simple ::simple-flag :complex ::type/complex-flag))
(s/def ::flags (s/coll-of ::flag))

(s/def ::type (s/keys :req-un [::base/name ::type/namespace] :opt-un [::flags]))
(s/def ::types (s/coll-of ::type))