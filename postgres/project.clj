(defproject hyperion/hyperion-postgres "3.7.1"
  :description "Postgres Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hyperion/hyperion-api "3.7.1"]
                 [hyperion/hyperion-sql "3.7.1"]
                 [postgresql/postgresql "8.4-702.jdbc4"]]

  :profiles {:dev {:dependencies [[speclj "2.7.5"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.7.5"]])
