(ns hyperion.riak.spec-helper
  (:require [speclj.core :refer :all ]
            [hyperion.riak :refer :all ]
            [hyperion.key :refer [generate-id]]
            [hyperion.api :refer [*ds*]]
            [clojure.set :refer [difference union]])
  (:import [com.basho.riak.client.query.indexes BinIndex KeyIndex]
           [com.basho.riak.client.raw.query.indexes BinRangeQuery]))

(defn create-bucket [client name]
  (.fetchBucket client name))

(defn empty-all-buckets [client]
  (doseq [bucket (.listBuckets client)]
    (doseq [key (.listKeys client bucket)]
      (.delete client bucket key))))

(defn with-testable-riak-datastore []
  (let [client (open-client :api :pbc )
        ds (new-riak-datastore client)]
    (list
      (before-all
        (empty-all-buckets client))
      (around [it]
        (binding [*ds* ds
                  *app* (generate-id)]
            (it)))
      (after-all
        (.shutdown client)))))
