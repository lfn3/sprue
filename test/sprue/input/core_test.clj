(ns sprue.input.core-test
  (:require [clojure.test :refer :all]
            [sprue.input.core :as ic]
            [sprue.test-util :as tu]))

(tu/use-standard-fixtures)

(deftest normalize-normalizes-flags
  (is (= (ic/normalize {:types []}) '()))
  (is (= (ic/normalize {:types [{:name "a" :namespace "ns" :fields [] :flags [:generate-id]}]})
         '({:name "a" :namespace "ns" :fields [] :flags ({:name :generate-id})})))
  (is (= (ic/normalize {:types [{:name "a" :namespace "ns" :fields [] :flags [{:name :base-type :test "thing"}]}]})
         '({:name "a" :namespace "ns" :fields [] :flags ({:name :base-type :test "thing"})}))))

(deftest normalize-merges-defaults
  (is (= (ic/normalize {:types [] :defaults {}}) '()))
  (is (= (ic/normalize {:types [] :defaults {:namespace "com.example"}}) '()))
  (is (= (ic/normalize {:types [{:name "a"}] :defaults {:namespace "com.example"}})
         '({:name "a" :namespace "com.example" :fields () :flags ()}))))

(deftest normalize-normalizes-field-flags
  (is (= (is (= (ic/normalize {:types [{:name      "a"
                                        :namespace "ns"
                                        :fields    [{:name "f" :type ["" ""] :flags [:const]}]}]})
                '({:name      "a"
                   :namespace "ns"
                   :flags     ()
                   :fields    ({:name "f" :type ["" ""] :flags ({:name :const})})}))))))