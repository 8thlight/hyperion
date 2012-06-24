(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-gae (:version config)
  :description "Google App Engine Datastore for Hyperion"
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-core ~(:version config)]
                 [com.google.appengine/appengine-api-1.0-sdk "1.6.6"]]
  :dev-dependencies [[speclj ~(:speclj-version config)]
                     [hyperion/hyperion-dev ~(:version config)]
                     [com.google.appengine/appengine-testing "1.6.6"]
                     [com.google.appengine/appengine-api-stubs "1.6.6"]]
  :test-path "spec")
