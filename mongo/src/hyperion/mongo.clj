(ns hyperion.mongo
  (:require [hyperion.abstr :refer [Datastore]]
            [hyperion.key :refer (compose-key decompose-key)]
            [chee.util :refer [->options]]))

(defn- ->address [spec]
  (let [host (first spec)
        port (second spec)]
    (com.mongodb.ServerAddress. host port)))

(defn address->seq [address]
  (list (.getHost address) (.getPort address)))

(defn- trusting-ssl-socket-factory []
  (let [trust-manager
        (proxy [javax.net.ssl.X509TrustManager] []
          (getAcceptedIssuers [this] nil)
          (checkClientTrusted [this certs type] nil)
          (checkServerTrusted [this certs type] nil))
        trust-managers (into-array javax.net.ssl.X509TrustManager [trust-manager])
        ssl-context (javax.net.ssl.SSLContext/getInstance "SSL")]
    (.init ssl-context nil trust-managers (java.security.SecureRandom.))
    (.getSocketFactory ssl-context)))

(defn- socket-factory [options]
  (when-let [ssl (:ssl options)]
    (if (= :trust ssl)
      (trusting-ssl-socket-factory)
      (javax.net.ssl.SSLSocketFactory/getDefault))))

(defn open-mongo [& args]
  (let [options (->options args)
        addresses (if (:host options) [(->address [(:host options) (or (:port options) 27017)])] [])
        addresses (doall (concat addresses (map ->address (:servers options))))
        mongo-options (com.mongodb.MongoOptions.)]
    (when (:ssl options) (.setSocketFactory mongo-options (socket-factory options)))
    (com.mongodb.Mongo. addresses mongo-options)))

(defn ->write-concern [value]
  (case (keyword value)
    :fsync-safe com.mongodb.WriteConcern/FSYNC_SAFE
    :journal-safe com.mongodb.WriteConcern/JOURNAL_SAFE
    :majority com.mongodb.WriteConcern/MAJORITY
    :none com.mongodb.WriteConcern/NONE
    :normal com.mongodb.WriteConcern/NORMAL
    :replicas-safe com.mongodb.WriteConcern/REPLICAS_SAFE
    :safe com.mongodb.WriteConcern/SAFE
    (throw (Exception. (str "Unknown write-concern: " value)))))

(defn open-database [mongo name & args]
  (let [options (->options args)
        db (.getDB mongo name)]
    (.setWriteConcern db (->write-concern (or (:write-concern options) :safe )))
    (when (:username options)
      (.authenticate db (:username options) (.toCharArray (:password options))))
    db))

(defn- ->db-object [record]
  (let [db-object (com.mongodb.BasicDBObject.)
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
    (com.mongodb.BasicDBObject.)
    (.put "_id" key)))

(defn- kv-dbo [key value]
  (doto
    (com.mongodb.BasicDBObject.)
    (.put key value)))

(defn- db-list [col]
  (doto
    (com.mongodb.BasicDBList.)
    (.addAll col)))

(defn- insert-records-of-kind [db kind records]
  (let [collection (.getCollection db kind)
        db-objects (mapv ->db-object records)
        result (.insert collection db-objects)]
    (if-let [error (.getError result)]
      (throw (Exception. (str "Failed to save record(s): " error)))
      (map (partial ->record kind) db-objects))))

(defn- update-record [db record]
  (let [kind (:kind record)
        collection (.getCollection db kind)
        db-object (->db-object record)
        result (.update collection (key-query (:key record)) db-object)]
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
  (let [[kind _] (decompose-key key)
        collection (.getCollection db kind)]
    (when-let [db-object (.findOne collection (key-query key))]
      (->record kind db-object))))

(defn- delete-by-key [db key]
  (let [[kind _] (decompose-key key)
        collection (.getCollection db kind)]
    (.remove collection (key-query key))))

(defn- ->query [[operator field value]]
  (case operator
    := (kv-dbo (name field) value)
    :> (kv-dbo (name field) (kv-dbo "$gt" value))
    :>= (kv-dbo (name field) (kv-dbo "$gte" value))
    :< (kv-dbo (name field) (kv-dbo "$lt" value))
    :<= (kv-dbo (name field) (kv-dbo "$lte" value))
    :contains? (kv-dbo (name field) (kv-dbo "$in" (db-list value)))
    :!= (kv-dbo (name field) (kv-dbo "$ne" value))))

(defn- build-query [filters]
  (let [queries (map ->query filters)]
    (cond
      (= 0 (count queries)) (com.mongodb.BasicDBList.)
      (= 1 (count queries)) (first queries)
      :else (kv-dbo "$and" (db-list queries)))))

(defn- build-sorts [sorts]
  (let [doc (com.mongodb.BasicDBObject.)]
    (doseq [[field direction] sorts]
      (.put doc (name field) (if (= :asc direction) 1 -1)))
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
  (let [collection (.getCollection db kind)]
    (.remove collection (build-query filters))))

(defn- count-by-kind [db kind filters]
  (let [collection (.getCollection db kind)]
    (.count collection (build-query filters))))

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
  (if (and (= 1 (count args)) (.isInstance com.mongodb.DB (first args)))
    (MongoDatastore. (first args))
    (let [options (->options args)
          db-name (:database options)
          _ (when (nil? db-name) (throw (Exception. "Missing :database entry.")))
          mongo (open-mongo options)
          db (open-database mongo db-name options)]
      (MongoDatastore. db))))

