(defproject hyperion/hyperion-mongo "3.6.0"
  :description "Mongo Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hyperion/hyperion-api "3.6.0"]
                 [org.mongodb/mongo-java-driver	"2.8.0"]]

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj "2.3.4"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.3.4"]])