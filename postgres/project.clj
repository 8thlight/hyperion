(defproject hyperion/hyperion-postgres "1.0.0"
  :description "A library for storing data."
  :dependencies [[org.clojure/clojure "1.4.0-beta6"]
                 [hyperion/hyperion-core "1.0.1"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [org.clojure/java.jdbc "0.1.1"]]
  :dev-dependencies [[speclj "2.1.1"]]
  :test-path "spec")
