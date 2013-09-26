(defproject hyperion/hyperion-redis "3.7.1"
  :description "Redis Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hyperion/hyperion-api "3.7.1"]
                 [com.taoensso/carmine "0.10.3"]]
  :profiles {:dev {:dependencies [[speclj "2.7.5"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.7.5"]])
