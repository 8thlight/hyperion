(ns hyperion.fake
  (:require
    [hyperion.abstr :refer [Datastore]]))

(defn- stub-call [ds name & params]
  (swap! (.calls ds) conj [name params])
  (let [result (first @(.responses ds))]
    (swap! (.responses ds) rest)
    result))

(deftype FakeDatastore [calls responses]
  Datastore
  (ds-save [this records] (stub-call this "ds-save" records))
  (ds-delete-by-key [this key] (stub-call this "ds-delete-by-key" key))
  (ds-delete-by-kind [this kind filters] (stub-call this "ds-delete-by-kind" kind filters))
  (ds-count-by-kind [this kind filters] (stub-call this "ds-count-by-kind" kind filters))
  (ds-find-by-key [this key] (stub-call this "ds-find-by-key" key))
  (ds-find-by-kind [this kind filters sorts limit offset] (stub-call this "ds-find-by-kind" kind filters sorts limit offset))
  (ds-all-kinds [this] (stub-call this "ds-all-kinds"))
  (ds-pack-key [this value] (stub-call this "ds-pack-key" value))
  (ds-unpack-key [this value] (stub-call this "ds-unpack-key" value)))

(defn new-fake-datastore []
  (FakeDatastore. (atom []) (atom [])))
