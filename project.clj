(defproject sprue "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.squareup/kotlinpoet "1.3.0"]
                 [com.squareup/javapoet "1.11.1"]]
  :main ^:skip-aot sprue.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
