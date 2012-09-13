(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-redis (:version config)
  :description "Redis Datastore for Hyperion"
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [hyperion/hyperion-api ~(:version config)]
                 [com.taoensso/carmine "0.10.3"]]

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj ~(:speclj-version config)]]}}
  :test-paths ["spec/"]
  :plugins [[speclj ~(:speclj-version config)]])

