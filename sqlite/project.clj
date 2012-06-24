(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-sqlite (:version config)
  :description "SQLite Datastore for Hyperion"
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-core ~(:version config)]
                 [hyperion/hyperion-sql ~(:version config)]
                 [sqlitejdbc "0.5.6"]]
  :dev-dependencies [[speclj ~(:speclj-version config)]
                     [hyperion/hyperion-dev ~(:version config)]]
  :test-path "spec")
