(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-sqlite (:version config)
  :description "SQLite Datastore for Hyperion"
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-api ~(:version config)]
                 [hyperion/hyperion-sql ~(:version config)]
                 [org.xerial/sqlite-jdbc "3.7.2"]]

  ; leiningen 1
  :dev-dependencies [[speclj ~(:speclj-version config)]]
  :test-path "spec"

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj ~(:speclj-version config)]]}}
  :test-paths ["spec/"]
  :plugins [[speclj ~(:speclj-version config)]])
