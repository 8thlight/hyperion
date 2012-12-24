(defproject hyperion/hyperion-gae "3.7.0"
  :description "Google App Engine Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hyperion/hyperion-api "3.7.0"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.6.6"]
                 [chee "1.1.0"]]

  ; leiningen 1
  :dev-dependencies [[speclj "2.5.0"]
                     [com.google.appengine/appengine-testing "1.6.6"]
                     [com.google.appengine/appengine-api-stubs "1.6.6"]]
  :test-path "spec"

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj "2.5.0"]
                                  [com.google.appengine/appengine-testing "1.6.6"]
                                  [com.google.appengine/appengine-api-stubs "1.6.6"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.5.0"]])
