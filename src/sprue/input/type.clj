(ns sprue.input.type
  (:require [sprue.input.base :as base]
            [sprue.input.field :as field]
            [clojure.spec.alpha :as s]))

(s/def ::simple-flag #{:generate-id
                       :generate-with-id})

(s/def ::name #{:base-type
                :implements})

(s/def ::complex-flag (s/keys :req-un [::name]))

(s/def ::flags (s/coll-of (s/or :simple ::simple-flag
                                :complex-flag ::complex-flag)))

(s/def ::namespace string?)

(s/def ::type (s/keys :req-un [::base/name]
                      :opt-un [::namespace                  ;Only optional because this can be supplied in defaults
                               ::flags
                               ::field/fields]))

(s/def ::types (s/coll-of ::type))