(ns hyperion.redis
  (:require [hyperion.abstr :refer [Datastore]]
            [hyperion.key :refer (compose-key)]
            [hyperion.sorting :as sort]
            [hyperion.filtering :as filter]
            [hyperion.memory :as memory]
            [taoensso.carmine :as r]
            [chee.util :refer [->options]]))

(def hyperion-namespace "hyperion:")

(defmacro carmine [db & body]
  `(r/with-conn (:pool ~db) (:spec ~db) ~@body))

(defn open-db [options]
  (let [defaults {:host "127.0.0.1" :port 6379 :password nil :timeout 0 :db 0}
        pool (r/make-conn-pool :max-active 8)
        connection-spec (merge defaults options)]
    {:pool pool :spec connection-spec}))

;; KEY LOOKUP

(defn- hyperion-key [key]
  (str hyperion-namespace key))

(defn- kind-key [kind]
  (str hyperion-namespace kind))

(defn- record-key [record]
  (let [key  (:key  record)]
    (hyperion-key key)))

(defn- keys-of-kind [db kind]
  (carmine db
    (r/smembers (kind-key kind))))

;; SAVING

(defn- add-keys-to-kind [db keys kind]
  (carmine db
    (doseq [key keys] (r/sadd (kind-key kind) key))
    (r/sadd (str hyperion-namespace "kinds") kind)))

(defn- save-record [db record]
  (carmine db
    (r/set (record-key record) record))
  record)

(defn- insert-records-of-kind [db kind records]
  (let [records (map #(assoc % :kind kind) records)
        records (map #(assoc % :key (compose-key kind)) records)]
    (add-keys-to-kind db (map :key records) kind)
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

;; FINDING

(defn- find-by-key [db key]
  (carmine db
    (r/get (hyperion-key key))))

(defn- find-by-kind [db kind filters sorts limit offset]
  (let [keys (keys-of-kind db kind)
        results (map (partial find-by-key db) keys)]
    (try
      (->> results
        (filter (memory/build-filter filters))
        (sort/sort-results sorts)
        (filter/offset-results offset)
        (filter/limit-results limit))
      (catch Exception e
        (prn (.getMessage e))
        (throw e)))))

;; DELETING

(defn- delete-by-key [db key]
  (let [record (find-by-key db key)
        kind (:kind record)]
    (carmine db
      (r/srem (kind-key kind) key)
      (r/del (hyperion-key key)))
    (carmine db
      (if (empty? (r/smembers (kind-key kind)))
        (r/srem (str hyperion-namespace "kinds") kind)))))

(defn- delete-by-kind [db kind filters]
  (let [keys (map :key (find-by-kind db kind filters nil nil nil))]
    (doseq [key keys] (delete-by-key db key))))

;; COUNTING

(defn- count-by-kind [db kind filters]
  (count (find-by-kind db kind filters nil nil nil)))

;; LISTING

(defn- list-all-kinds [db]
  (carmine db
    (r/smembers (str hyperion-namespace "kinds"))))

;; DATASTORE

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
