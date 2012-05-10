(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-core (:version config)
  :description "A library for storing data."
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [chee ~(:chee-version config)]]
  :dev-dependencies [[speclj ~(:speclj-version config)]]
  :test-path "spec")
