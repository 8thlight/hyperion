(ns hyperion.fake
  (:use
    [hyperion.core]))

(defn- stub-call [ds name & params]
  (swap! (.calls ds) conj [name params])
  (let [result (first @(.responses ds))]
    (swap! (.responses ds) rest)
    result))

(deftype FakeDatastore [calls responses]
  Datastore
  (ds-save [this records] (stub-call this "ds-save" records))
  (ds-delete-by-id [this kind id] (stub-call this "ds-delete-by-id" kind id))
  (ds-delete-by-kind [this kind filters] (stub-call this "ds-delete-by-kind" kind filters))
  (ds-count-by-kind [this kind filters] (stub-call this "ds-count-by-kind" kind filters))
  (ds-find-by-id [this kind id] (stub-call this "ds-find-by-id" kind id))
  (ds-find-by-kind [this kind filters sorts limit offset] (stub-call this "ds-find-by-kind" kind filters sorts limit offset))
  (ds-all-kinds [this] (stub-call this "ds-all-kinds")))

(defn new-fake-datastore []
  (FakeDatastore. (atom []) (atom [])))
