(ns sprue.input.field
  (:require [clojure.spec.alpha :as s]
            [sprue.input.base :as base]))

(s/def ::simple-flag #{:const :optional})

(s/def ::name #{})

(s/def ::complex-flag (s/keys :req-un [::name]))

(s/def ::flags (s/coll-of (s/or :simple ::simple-flag
                                :complex-flag ::complex-flag)))

(s/def ::field (s/keys :req-un [::base/name ::base/type]
                       :opt-un [::flags]))

(s/def ::fields (s/coll-of ::field))

