(ns hyperion.memory
  (:use
    [hyperion.core]))

(defn- create-key []
  (str (java.util.UUID/randomUUID)))

(defn- save-record [ds record]
  (let [record (if (new? record) (assoc record :key (create-key)) record)]
    (dosync
      (alter (.store ds) assoc (:key record) record))
    record))

(defn- find-record-by-key [ds key]
  (get @(.store ds) key))

(defn- delete-records [ds keys]
  (dosync
    (apply alter (.store ds) dissoc keys)))

(defn- find-records-by-kind [ds kind filters sorts limit offset]
  (filter #(= kind (:kind %)) (vals @(.store ds))))

(deftype MemoryDatastore [store]
  Datastore
  (ds-save [this record] (save-record this record))
  (ds-save* [this records] (for [record records] (save-record this record)))
  (ds-delete [this keys] (delete-records this keys))
  (ds-find-by-key [this key] (find-record-by-key this key))
  (ds-find-by-kind [this kind filters sorts limit offset] (find-records-by-kind this kind filters sorts limit offset))
  )

(defn new-memory-datastore []
  (MemoryDatastore. (ref {})))