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

(defn- delete-record [ds key]
  (dosync
    (alter (.store ds) dissoc key)))

(defn- find-records-by-kind [ds kind filters sorts]
  (filter #(= kind (:kind %)) (vals @(.store ds))))

(deftype MemoryDatastore [store]
  Datastore
  (ds-save [this record] (save-record this record))
  (ds-find-by-key [this key] (find-record-by-key this key))
  (ds-delete [this key] (delete-record this key))
  (ds-find-by-kind [this kind filters sorts] (find-records-by-kind this kind filters sorts))
  )

(defn new-memory-datastore []
  (MemoryDatastore. (ref {})))