(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-gae (:version config)
  :description "A library for storing data."
  :repositories {"releases" "http://gaeshi-mvn.googlecode.com/svn/trunk/releases/"}

  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-core ~(:version config)]
                 [com.google.appengine/appengine-api-1.0-sdk "1.6.2.1"]]
  :dev-dependencies [[speclj ~(:speclj-version config)]
                     [com.google.appengine/appengine-api-labs "1.6.2.1"]
                     [com.google.appengine/appengine-api-stubs "1.6.2.1"]
                     [com.google.appengine/appengine-local-runtime "1.6.2.1"]
                     [com.google.appengine/appengine-local-runtime-shared "1.6.2.1"]
                     [com.google.appengine/appengine-testing "1.6.2.1"]]
  :test-path "spec")