(def config (load-file "../config.clj"))

(defproject hyperion/hyperion-api (:version config)
  :description "A generic persistence API for Clojure"
  :dependencies [[org.clojure/clojure ~(:clojure-version config)]
                 [chee ~(:chee-version config)]
                 [org.clojure/data.codec "0.1.0"]]

  ; leiningen 1
  :dev-dependencies [[speclj ~(:speclj-version config)]]
  :test-path "spec"

  ; leiningen 2
  :profiles {:dev {:dependencies [[speclj ~(:speclj-version config)]]}}
  :test-paths ["spec/"]
  :plugins [[speclj ~(:speclj-version config)]
            [lein-autodoc "0.9.0"]])
