(ns sprue.java-output-test
  (:require [clojure.test :refer :all]
            [sprue.java-output :as jo]
            [sprue.test-util :as tu]))

(tu/use-standard-fixtures)

(deftest test-get-instance-fields
  (is (empty? (jo/get-instance-fields [{:name "id", :type long}]
                                      [{:name :base-type, :base-type ["" ""], :super-ctor-fields #{"id"}}]))))

(deftest test-get-supertype-fields
  (is (empty? (jo/get-supertype-fields [{:name "id", :type long}]
                                       [{:name :base-type, :base-type ["" ""], :super-ctor-fields #{}}])))
  (is (empty? (jo/get-supertype-fields [{:name "id", :type long}]
                                       [{:name :base-type, :base-type ["" ""]}])))
  (let [field {:name "id", :type long}
        result (jo/get-supertype-fields [field]
                                        [{:name :base-type, :base-type ["" ""] :super-ctor-fields #{"id"}}])]
    (is (= (count result) 1))
    (is (= field (first result)))))
