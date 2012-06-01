(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-postgres (:version config)
  :description "A library for storing data."
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-core ~(:version config)]
                 [org.clojure/java.jdbc "0.1.1"]
                 [postgresql/postgresql "8.4-702.jdbc4"]]
  :dev-dependencies [[speclj ~(:speclj-version config)]]
  :test-path "spec")
