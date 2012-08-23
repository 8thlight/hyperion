(ns hyperion.mongo.spec-helper
  (:require [speclj.core :refer :all ]
            [hyperion.mongo :refer :all ]
            [hyperion.core :refer [*ds*]]
            [clojure.set :refer [difference union]]))

(def test-mongo (atom nil))

(defn- clear-db [db]
  (let [collection-names (filter #(not (.startsWith % "system.")) (.getCollectionNames db))]
    (doseq [collection (map #(.getCollection db %) collection-names)]
      (.drop collection))))

(defn with-testable-mongo-datastore []
  (list
    (before-all (reset! test-mongo (open-mongo :host "localhost" :port 27017)))
    (after-all (.close @test-mongo))
    (around [it]
      (let [db (open-database @test-mongo "hyperion-test")
            ds (new-mongo-datastore db)]
        (binding [*ds* ds]
          (try
            (it)
            (finally
              (clear-db db))))))))


