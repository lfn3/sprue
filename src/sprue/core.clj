(ns sprue.core
  (:require [sprue.kotlin-output :as kotlin]
            [sprue.specs :as specs]
            [sprue.java-output :as java]
            [clojure.string :as str])
  (:import (java.io StringWriter))
  (:gen-class))

(defn -main
  [& _]
  (println "This is designed to be used with a repl at present."))
