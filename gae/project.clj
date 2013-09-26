(defproject hyperion/hyperion-gae "3.7.1"
  :description "Google App Engine Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hyperion/hyperion-api "3.7.1"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.6.6"]
                 [chee "1.1.0"]]
  :profiles {:dev {:dependencies [[speclj "2.7.5"]
                                  [com.google.appengine/appengine-testing "1.6.6"]
                                  [com.google.appengine/appengine-api-stubs "1.6.6"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.7.5"]])

