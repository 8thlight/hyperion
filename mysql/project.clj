(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-mysql (:version config)
  :description "MySQL Datastore for Hyperion"
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-core ~(:version config)]
                 [hyperion/hyperion-sql ~(:version config)]
                 [mysql/mysql-connector-java "5.1.6"]]
  :dev-dependencies [[speclj ~(:speclj-version config)]
                     [hyperion/hyperion-dev ~(:version config)]]
  :test-path "spec")
