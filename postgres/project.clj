(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-postgres (:version config)
  :description "A library for storing data."
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-core ~(:version config)]
                 [hyperion/hyperion-sql ~(:version config)]
                 [postgresql/postgresql "8.4-702.jdbc4"]]
  :dev-dependencies [[speclj ~(:speclj-version config)]]
  :test-path "spec")
