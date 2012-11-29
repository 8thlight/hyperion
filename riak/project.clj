(defproject hyperion/hyperion-riak "3.4.1"
  :description "Riak Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hyperion/hyperion-api "3.4.1"]
                 [com.basho.riak/riak-client "1.0.5"]
                 [cheshire "4.0.1"]]

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj "2.3.4"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.3.4"]])


