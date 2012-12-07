(defproject hyperion/hyperion-sqlite "3.6.1"
  :description "SQLite Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hyperion/hyperion-api "3.6.1"]
                 [hyperion/hyperion-sql "3.6.1"]
                 [org.xerial/sqlite-jdbc "3.7.2"]]

  ; leiningen 1
  :dev-dependencies [[speclj "2.4.0"]]
  :test-path "spec"

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj "2.4.0"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.4.0"]])
