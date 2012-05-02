(ns hyperion.fake
  (:use
    [hyperion.core]))

(defn- stub-call [ds name & params]
  (swap! (.calls ds) conj [name params])
  (let [result (first @(.responses ds))]
    (swap! (.responses ds) rest)))

(deftype FakeDatastore [calls responses]
  Datastore
  (ds-map->entity [this record] (stub-call this "ds-map->entity" record))
  (ds-save [this record] (stub-call this "ds-save" record))
  (ds-delete [this key] (stub-call this "ds-delete" key))
  (ds-count-by-kind [this kind filters] (stub-call this "ds-count-by-kind" kind filters))
  (ds-count-all-kinds [this filters] (stub-call this "ds-count-all-kinds" filters))
  (ds-find-by-key [this key] (stub-call this "ds-find-by-key" key))
  (ds-find-by-kind [this kind filters sorts limit offset] (stub-call this "ds-find-by-kind" kind filters sorts limit offset))
  (ds-find-all-kinds [this filters sorts limit offset] (stub-call this "ds-find-all-kinds" filters sorts limit offset))
  )

(defn new-fake-datastore []
  (FakeDatastore. (atom []) (atom [])))