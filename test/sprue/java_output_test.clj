(ns sprue.java-output-test
  (:require [clojure.test :refer :all]
            [sprue.java-output :as jo]))

(deftest test-get-instance-fields
  (is (empty? (jo/get-instance-fields [{:name "id", :type long}]
                                      [{:name :base-type, :base-type ["" ""], :super-ctor-fields #{"id"}}]))))