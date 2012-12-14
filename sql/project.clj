(defproject hyperion/hyperion-sql "3.7.0"
  :description "SQL utilities for hyperion"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hyperion/hyperion-api "3.7.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [c3p0/c3p0 "0.9.1.2"]]

  ; leiningen 1
  :dev-dependencies [[speclj "2.4.0"]
                     [chee "1.0.0"]
                     [org.xerial/sqlite-jdbc "3.7.2"]]
  :test-path "spec"


  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj "2.4.0"]
                                  [chee "1.0.0"]
                                  [org.xerial/sqlite-jdbc "3.7.2"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.4.0"]])
