(defproject hyperion/hyperion-redis "3.6.1"
  :description "Redis Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hyperion/hyperion-api "3.6.1"]
                 [com.taoensso/carmine "0.10.3"]]

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj "2.4.0"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.4.0"]])
