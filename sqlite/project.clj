(defproject hyperion/hyperion-sqlite "3.7.1"
  :description "SQLite Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hyperion/hyperion-api "3.7.1"]
                 [hyperion/hyperion-sql "3.7.1"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [chee "1.1.0"]]
  :profiles {:dev {:dependencies [[speclj "2.7.5"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.7.5"]])
