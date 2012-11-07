(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-mongo "3.4.2"
  :description "Mongo Datastore for Hyperion"
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-api ~(:version config)]
                 [org.mongodb/mongo-java-driver	"2.8.0"]]

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj ~(:speclj-version config)]]}}
  :test-paths ["spec/"]
  :plugins [[speclj ~(:speclj-version config)]])