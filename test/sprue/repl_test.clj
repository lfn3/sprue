(ns sprue.repl-test
  (:require [clojure.test :refer :all]
            [sprue.repl]
            [clojure.java.io :as io]
            [sprue.test-util :as tu]))

(tu/use-standard-fixtures)

(def test-namespace "net.lfn3.sprue")

(defn actual-entity []
  (-> (sprue.repl/entity-type* "Bruce"
                               [{:name "aField" :type BigDecimal :flags [:optional]}]
                               test-namespace
                               "lfn3_"
                               "_id")
      (update :flags conj {:name :base-type :base-type [test-namespace "Batman"]})
      (update :flags conj {:name :implements :interface-type [test-namespace "Dated"]})))

(def entity-path "repl_test/Bruce.java")

(defn regen-entity [] (sprue.repl/barf-generated (actual-entity) (io/resource entity-path)))

(deftest entity-compat
  (let [actual (sprue.repl/show-generated (actual-entity))
        expected (slurp (io/resource entity-path))]
    (is (= actual expected))))

(defn actual-id []
  (sprue.repl/id-type* "Bruce" test-namespace ["net.lfn3.sprue" "BaseId"]))

(def id-path "repl_test/BruceId.java")

(defn regen-id [] (sprue.repl/barf-generated (actual-id) (io/resource id-path)))

(deftest id-compat
  (let [actual (sprue.repl/show-generated (actual-id))
        expected (slurp (io/resource id-path))]
    (is (= actual expected))))