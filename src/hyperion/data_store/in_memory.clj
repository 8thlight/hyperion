(ns hyperion.data-store.in-memory
  (:use [hyperion.data-store :only [DataStore]])
  (:require [clojure.set]))

(defn is-subset? [record matcher]
  (empty? (clojure.set/difference (set matcher) (set record))))

(defn find-records [records matcher]
  (filter #(is-subset? % matcher) records))

(defn- set-id [collection-key id id-counters]
  (swap! id-counters (fn [old-data] (assoc old-data collection-key id))))

(defn- make-new-id [collection-key id-counters]
  (let [new-id (inc (collection-key @id-counters))]
    (set-id collection-key new-id id-counters)
    new-id))

(defn- create-record-vector [collection-key data-saved]
  (swap! data-saved (fn [old-data] (assoc old-data collection-key []))))

(defn- ensure-record-vector [collection-key data-saved id-counters]
  (when (nil? (collection-key @data-saved))
    (create-record-vector collection-key data-saved)
    (set-id collection-key 0 id-counters)))

(defn- create-record [collection-key record id-counters data-saved]
  (ensure-record-vector collection-key data-saved id-counters)
  (let [new-record (merge record {:id (make-new-id collection-key id-counters)})]
    (swap! data-saved
      (fn [old-data]
        (update-in old-data [collection-key]
          (fn [old-type-records]
            (conj old-type-records new-record)))))
    new-record))

(defn- update-record [collection-key id new-value id-counters data-saved]
  (swap! data-saved
    (fn [old-data]
      (update-in old-data [collection-key]
        (fn [old-type-records]
          (map #(if (= (:id %) id) new-value %) old-type-records)))))
  new-value)

(defn- delete-record [collection-key id id-counters data-saved]
  (swap! data-saved
    (fn [old-data]
      (update-in old-data [collection-key]
        (fn [old-type-records]
          (remove #(= (:id %) id) old-type-records))))))


(deftype InMemoryPersistor [id-counter data-saved]
  DataStore
  (create [this collection-name record]
    (create-record (keyword collection-name) record id-counter data-saved))
  (find-where [this collection-name attrs]
    (find-records ((keyword collection-name) @data-saved) attrs))
  (update [this collection-name record]
    (update-record (keyword collection-name) (:id record) record id-counter data-saved))
  (delete [this collection-name record]
    (delete-record (keyword collection-name) (:id record) id-counter data-saved)))

(defn new-in-memory-persistor []
  (InMemoryPersistor. (atom {}) (atom {})))

