(ns hyperion.gae
  (:use
    [joodo.string :only (gsub spear-case)]
    [joodo.core :only (->options)]
    [gaeshi.datastore.types :only (pack unpack)])
  (:require
    [clojure.string :as str])
  (:import
    [com.google.appengine.api.datastore Entity Query DatastoreServiceFactory Query$FilterOperator
     Query$SortDirection FetchOptions$Builder EntityNotFoundException KeyFactory Key])

(deftype MemoryDatastore [store]
  Datastore
  (ds->kind [this thing] (if (string? thing) thing nil))
  (ds->ds-key [this thing] (if (string? thing) thing nil))
  (ds->string-key [this thing] thing)
  (ds-save [this record] (save-record this record))
  (ds-save* [this records] (doall (for [record records] (save-record this record))))
  (ds-delete [this keys] (delete-records this keys))
  (ds-count-by-kind [this kind filters] (count (find-records-by-kind this kind filters nil nil nil)))
  (ds-count-all-kinds [this filters] (count (find-records this filters nil nil nil)))
  (ds-find-by-key [this key] (find-record-by-key this key))
  (ds-find-by-keys [this keys] (map #(find-record-by-key this %) keys))
  (ds-find-by-kind [this kind filters sorts limit offset] (find-records-by-kind this kind filters sorts limit offset))
  (ds-find-all-kinds [this filters sorts limit offset] (find-records this filters sorts limit offset))
  (ds-native->entity [this entity] entity)
  (ds-entity->native [this map] map)
  )

(defn new-memory-datastore
  ([] (MemoryDatastore. (ref {})))
  ([stuff] (MemoryDatastore. (ref stuff))))