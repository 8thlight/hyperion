(ns hyperion.riak.spec-helper
  (:require [speclj.core :refer :all ]
            [hyperion.riak :refer :all ]
            [hyperion.api :refer [*ds*]]
            [clojure.set :refer [difference union]])
  (:import [com.basho.riak.client.query.indexes BinIndex KeyIndex]
           [com.basho.riak.client.raw.query.indexes BinRangeQuery]))

(defn empty-all-buckets [client]
  (let [buckets (.listBuckets client)
        buckets (filter #(.startsWith % "_HTEST_") buckets)]
    (doseq [bucket buckets]
      (doseq [key (.fetchIndex client (BinRangeQuery. KeyIndex/index bucket "0" "zzzz"))]
        (.delete client bucket key)))))

(defn with-testable-riak-datastore []
  (around [it]
    (let [client (open-client :api :pbc )
          ds (new-riak-datastore client)]
      (binding [*ds* ds
                *app* "_HTEST_"]
        (try
          (it)
          (finally (empty-all-buckets client)))
        (.shutdown client)))))
