(ns hyperion.redis
  (:require [hyperion.abstr :refer [Datastore]]
            [hyperion.key :refer (compose-key)]
            [hyperion.sorting :as sort]
            [hyperion.filtering :as filter]
            [hyperion.memory :as memory]
            [taoensso.carmine :as r]
            [chee.util :refer [->options]]))

(defmacro carmine [db & body]
  `(r/with-conn (:pool ~db) (:spec ~db) ~@body))

(defn open-db [options]
  (let [defaults {:host "127.0.0.1" :port 6379 :password nil :timeout 0 :db 0}
        pool (r/make-conn-pool :max-active 8)
        connection-spec (merge defaults options)]
    {:pool pool :spec connection-spec}))

(defn- kind-key [kind]
  (str kind ":*"))

(defn- find-full-key [db key]
  (first (carmine db (r/keys (str "*:" key)))))

(defn- save-record [db record]
  (let [kind (:kind record)
        key (name (:key record))]
    (carmine db
      (r/set (str kind ":" key) record))
    record))

(defn- insert-records-of-kind [db kind records]
  (let [records (map #(assoc % :kind kind) records)
        records (map #(assoc % :key (compose-key kind)) records)]
    (mapv (partial save-record db) records)))

(defn- save-records [db records]
  (let [inserts (filter #(nil? (:key %)) records)
        updates (filter :key records)
        insert-groups (group-by :kind inserts)]
    (doall
      (concat
        (map (partial save-record db) updates)
        (mapcat
          (fn [[kind values]] (insert-records-of-kind db kind values))
          insert-groups)))))

(defn- find-by-key [db key]
  (let [key (find-full-key db key)]
    (carmine db (r/get key))))

(defn- find-by-kind [db kind filters sorts limit offset]
  (let [keys (carmine db (r/keys (kind-key kind)))
        results (map (fn [key] (carmine db (r/get key))) keys)]
    (->> results
      (filter (memory/build-filter filters))
      (sort/sort-results sorts)
      (filter/offset-results offset)
      (filter/limit-results limit))))

(defn- delete-by-key [db key]
  (let [key (find-full-key db key)]
    (carmine db (r/del key))))

(defn- delete-by-kind [db kind filters]
  (let [keys (map :key (find-by-kind db kind filters nil nil nil))]
    (doseq [key keys] (delete-by-key db key))))

(defn- count-by-kind [db kind filters]
  (count (find-by-kind db kind filters nil nil nil)))

(defn- list-all-kinds [db]
  (let [keys (carmine db (r/keys "*"))]
    (vec (set (map #(first (clojure.string/split % #":")) keys)))))

(deftype RedisDatastore [db]
  Datastore
  (ds-save [this records] (save-records db records))
  (ds-delete-by-kind [this kind filters] (delete-by-kind db kind filters))
  (ds-delete-by-key [this key] (delete-by-key db key))
  (ds-count-by-kind [this kind filters] (count-by-kind db kind filters))
  (ds-find-by-key [this key] (find-by-key db key))
  (ds-find-by-kind [this kind filters sorts limit offset] (find-by-kind db kind filters sorts limit offset))
  (ds-all-kinds [this] (list-all-kinds db))
  (ds-pack-key [this value] value)
  (ds-unpack-key [this kind value] value))

(defn new-redis-datastore [& args]
  (let [options (->options args)
        db (open-db options)]
    (RedisDatastore. db)))
