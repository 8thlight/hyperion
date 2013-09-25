(defproject hyperion/hyperion-mongo "3.7.0"
  :description "Mongo Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hyperion/hyperion-api "3.7.0"]
                 [org.mongodb/mongo-java-driver "2.8.0"]]

  :profiles {:dev {:dependencies [[speclj "2.7.5"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.7.5"]])
