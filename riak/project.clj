(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-riak (:version config)
  :description "Riak Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hyperion/hyperion-core ~(:version config)]
                 [com.basho.riak/riak-client "1.0.5"]
                 [cheshire "4.0.1"]]

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj ~(:speclj-version config)]
                                  [hyperion/hyperion-dev ~(:version config)]]}}
  :test-paths ["spec/"]
  :plugins [[speclj ~(:speclj-version config)]])


