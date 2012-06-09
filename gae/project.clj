(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-gae (:version config)
  :description "A library for storing data."
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-core ~(:version config)]
                 [com.google.appengine/appengine-api-1.0-sdk "1.6.6"]]
  :dev-dependencies [[speclj ~(:speclj-version config)]
                     [com.google.appengine/appengine-api-stubs "1.6.6"]
                     [com.google.appengine/appengine-testing "1.6.6"]
                     [hyperion/hyperion-dev ~(:version config)]]
  :test-path "spec")
