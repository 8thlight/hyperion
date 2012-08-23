(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-mongo (:version config)
  :description "Mongo Datastore for Hyperion"
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-core ~(:version config)]
                 [org.mongodb/mongo-java-driver	"2.8.0"]]

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj ~(:speclj-version config)]]}}
  :test-paths ["spec/"]
  :plugins [[speclj ~(:speclj-version config)]])