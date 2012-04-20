(def config (load-file "../config.clj"))

(defproject spec (:version config)
  :description "The specification for a datastore"
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-memory ~(:version config)]
                 [hyperion/hyperion-postgres ~(:version config)]]
  :dev-dependencies [[speclj ~(:speclj-version config)]]
  :test-path "spec")
