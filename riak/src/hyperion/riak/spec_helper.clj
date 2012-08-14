(ns hyperion.riak.spec-helper
  (:require [speclj.core :refer :all ]
            [hyperion.riak :refer :all ]
            [hyperion.core :refer [*ds*]]))

(defn empty-test-bucket [client]
  (doseq [key (iterator-seq (.iterator (.listKeys client "_test_")))]
    (.delete client "_test_" key)))

(defn with-testable-riak-datastore []
  (around [it]
    (let [client (open-client :api :http )
          ds (new-riak-datastore client)]
      (binding [*ds* ds
                *bucket* "_test_"]
        (try
          (it)
          (finally (empty-test-bucket client)))
        (.shutdown client)))))