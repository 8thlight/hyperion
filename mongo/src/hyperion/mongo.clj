(ns hyperion.mongo
  (:require [chee.util :refer [->options]]
            [hyperion.abstr :refer [Datastore]]
            [hyperion.key :refer (compose-key decompose-key)]
            [hyperion.log :as log]
            [hyperion.filtering :as filter]
            [hyperion.sorting :as sort]
            [hyperion.mongo.types])
  (:import  [com.mongodb ServerAddress MongoOptions Mongo WriteConcern BasicDBObject BasicDBList DB]
            [javax.net.ssl SSLContext X509TrustManager SSLSocketFactory]
            [java.security SecureRandom]))

(defn- ->address [spec]
  (let [host (first spec)
        port (second spec)]
    (ServerAddress. host port)))

(defn address->seq [address]
  (list (.getHost address) (.getPort address)))

(def trust-manager
  (proxy [X509TrustManager] []
    (getAcceptedIssuers [] nil)
    (checkClientTrusted [certs type])
    (checkServerTrusted [certs type])))

(def trust-managers (into-array X509TrustManager [trust-manager]))

(defn- trusting-ssl-socket-factory []
  (let [ssl-context (SSLContext/getInstance "SSL")]
    (.init ssl-context nil trust-managers (SecureRandom.))
    (.getSocketFactory ssl-context)))

(defn- socket-factory [ssl]
  (if (= :trust ssl)
    (trusting-ssl-socket-factory)
    (SSLSocketFactory/getDefault)))

(defn open-mongo [& args]
  (let [{:keys [host port servers ssl] :or {port 27017}} (->options args)
        addresses (if host [(->address [host port])] [])
        addresses (doall (concat addresses (map ->address servers)))
        mongo-options (MongoOptions.)]
    (when ssl (.setSocketFactory mongo-options (socket-factory ssl)))
    (Mongo. addresses mongo-options)))

(defn ->write-concern [value]
  (case (keyword value)
    :fsync-safe WriteConcern/FSYNC_SAFE
    :journal-safe WriteConcern/JOURNAL_SAFE
    :majority WriteConcern/MAJORITY
    :none WriteConcern/NONE
    :normal WriteConcern/NORMAL
    :replicas-safe WriteConcern/REPLICAS_SAFE
    :safe WriteConcern/SAFE
    (throw (Exception. (str "Unknown write-concern: " value)))))

(defn open-database [mongo name & args]
  (let [{:keys [write-concern username password] :or {write-concern :safe} :as options} (->options args)
        db (.getDB mongo name)]
    (.setWriteConcern db (->write-concern write-concern))
    (when username
      (.authenticate db username (.toCharArray password)))
    db))

(defn- ->db-object [record]
  (let [db-object (BasicDBObject.)
        key (or (:key record) (compose-key (:kind record)))]
    (.put db-object "_id" key)
    (doseq [[k v] (dissoc record :key :kind )]
      (.put db-object (name k) v))
    db-object))

(defn- ->record [kind db-object]
  (reduce
    (fn [record key]
      (if (= "_id" key)
        (assoc record :key (.get db-object "_id"))
        (assoc record (keyword key) (.get db-object key))))
    {:kind kind}
    (.keySet db-object)))

(defn- key-query [key]
  (doto
    (BasicDBObject.)
    (.put "_id" key)))

(defn- kv-dbo [key value]
  (doto
    (BasicDBObject.)
    (.put key value)))

(defn- db-list [col]
  (doto
    (BasicDBList.)
    (.addAll col)))

(defn- insert-records-of-kind [db kind records]
  (let [collection (.getCollection db kind)
        db-objects (mapv ->db-object records)
        result (.insert collection db-objects)]
    (if-let [error (.getError result)]
      (throw (Exception. (str "Failed to save record(s): " error)))
      (map (partial ->record kind) db-objects))))

(defn- update-record [db {:keys [kind key] :as record}]
  (let [collection (.getCollection db kind)
        db-object (->db-object record)
        result (.update collection (key-query key) db-object)]
    (->record kind db-object)))

(defn- save-records [db records]
  (let [inserts (filter #(nil? (:key %)) records)
        updates (filter :key records)
        insert-groups (group-by :kind inserts)]
    (doall
      (concat
        (map (partial update-record db) updates)
        (mapcat
          (fn [[kind values]] (insert-records-of-kind db kind values))
          insert-groups)))))

(defn- find-by-key [db key]
  (try
    (let [[kind _] (decompose-key key)
          collection (.getCollection db kind)]
      (when-let [db-object (.findOne collection (key-query key))]
        (->record kind db-object)))
    (catch Exception e
      (log/warn (format "find-by-key error: %s" (.getMessage e)))
      nil)))

(defn- delete-by-key [db key]
  (try
    (let [[kind _] (decompose-key key)
          collection (.getCollection db kind)]
      (.remove collection (key-query key)))
    (catch Exception e
      (log/warn (format "delete-by-key error: %s" (.getMessage e)))
      nil)))

(defn- ->query [filter]
  (let [field (filter/field filter)
        value (filter/value filter)]
    (case (filter/operator filter)
      := (kv-dbo (name field) value)
      :> (kv-dbo (name field) (kv-dbo "$gt" value))
      :>= (kv-dbo (name field) (kv-dbo "$gte" value))
      :< (kv-dbo (name field) (kv-dbo "$lt" value))
      :<= (kv-dbo (name field) (kv-dbo "$lte" value))
      :contains? (kv-dbo (name field) (kv-dbo "$in" (db-list value)))
      :!= (kv-dbo (name field) (kv-dbo "$ne" value)))))

(defn- build-query [filters]
  (let [queries (map ->query filters)]
    (cond
      (= 0 (count queries)) (BasicDBList.)
      (= 1 (count queries)) (first queries)
      :else (kv-dbo "$and" (db-list queries)))))

(defn- build-sorts [sorts]
  (let [doc (BasicDBObject.)]
    (doseq [sort sorts]
      (.put doc (name (sort/field sort)) (if (= :asc (sort/order sort)) 1 -1)))
    doc))

(defn- find-by-kind [db kind filters sorts limit offset]
  (let [collection (.getCollection db kind)
        query (build-query filters)
        cursor (.find collection query)]
    (when limit (.limit cursor limit))
    (when offset (.skip cursor offset))
    (when (seq sorts) (.sort cursor (build-sorts sorts)))
    (map (partial ->record kind) (iterator-seq (.iterator cursor)))))

(defn- delete-by-kind [db kind filters]
  (-> (.getCollection db kind)
    (.remove (build-query filters))))

(defn- count-by-kind [db kind filters]
  (-> (.getCollection db kind)
    (.count (build-query filters))))

(defn- list-all-kinds [db]
  (filter #(not (.startsWith % "system.")) (.getCollectionNames db)))

(deftype MongoDatastore [^com.mongodb.DB db]
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

(defn new-mongo-datastore [& args]
  (if (and (= 1 (count args)) (.isInstance DB (first args)))
    (MongoDatastore. (first args))
    (let [{:keys [database] :as options} (->options args)
          _ (when (nil? database) (throw (Exception. "Missing :database entry.")))
          mongo (open-mongo options)
          db (open-database mongo database options)]
      (MongoDatastore. db))))

