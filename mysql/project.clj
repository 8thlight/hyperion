(defproject hyperion/hyperion-mysql "3.7.0"
  :description "MySQL Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hyperion/hyperion-api "3.7.0"]
                 [hyperion/hyperion-sql "3.7.0"]
                 [mysql/mysql-connector-java "5.1.6"]]

  ; leiningen 1
  :dev-dependencies [[speclj "2.4.0"]]
  :test-path "spec"

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj "2.4.0"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.4.0"]])
