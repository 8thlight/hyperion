(defproject hyperion/hyperion-mysql "3.4.1"
  :description "MySQL Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hyperion/hyperion-api "3.4.1"]
                 [hyperion/hyperion-sql "3.4.1"]
                 [mysql/mysql-connector-java "5.1.6"]]

  ; leiningen 1
  :dev-dependencies [[speclj "2.3.4"]]
  :test-path "spec"

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj "2.3.4"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.3.4"]])
