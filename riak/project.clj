(defproject hyperion/hyperion-riak "3.7.0"
  :description "Riak Datastore for Hyperion"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hyperion/hyperion-api "3.7.0"]
                 [com.basho.riak/riak-client "1.0.5"]
                 [cheshire "5.0.1"]
                 [fleet "0.9.5"]
                 [com.google.javascript/closure-compiler "r2388"]]
  :profiles {:dev {:dependencies [[speclj "2.7.5"]]}}
  :test-paths ["spec/"]
  :plugins [[speclj "2.7.5"]])


