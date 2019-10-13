(ns sprue.test-util
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as s.test]
            [orchestra.spec.test :as o.test]
            [clojure.test :as t]))

(defn once-fixtures [f]
  (o.test/instrument)
  (f)
  (o.test/unstrument))

(defn use-standard-fixtures []
  (t/use-fixtures :once once-fixtures)
  ;(t/use-fixtures :each each-fixtures)
  )