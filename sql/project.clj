(defproject hyperion/hyperion-sql "3.7.1"
  :description "SQL utilities for hyperion"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hyperion/hyperion-api "3.7.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [c3p0/c3p0 "0.9.1.2"]]
  :profiles {:dev {:dependencies [[speclj "2.7.5"]
                                  [chee "1.0.0"]
                                  [org.xerial/sqlite-jdbc "3.7.2"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.7.5"]])
