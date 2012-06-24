(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-sql (:version config)
  :description "SQL utilities for hyperion"
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-core ~(:version config)]]
  :dev-dependencies [[speclj ~(:speclj-version config)]
                     [chee ~(:chee-version config)]
                     [sqlitejdbc "0.5.6"]]
  :test-path "spec")
