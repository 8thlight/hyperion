(ns hyperion.gae
  (:require [chee.string :refer [gsub spear-case]]
            [chee.util :refer [->options]]
            [clojure.string :as str]
            [hyperion.log :as log]
            [hyperion.abstr :refer [Datastore]]
            [hyperion.filtering :as filter]
            [hyperion.gae.types]
            [hyperion.sorting :as sort])
  (:import [com.google.appengine.api.datastore Entity Entities Query DatastoreService DatastoreServiceFactory Query$FilterOperator
            Query$SortDirection FetchOptions$Builder EntityNotFoundException KeyFactory Key]
            [java.lang IllegalArgumentException]))

(defn create-key [kind id]
  (if (number? id)
    (KeyFactory/createKey kind (long id))
    (KeyFactory/createKey kind (str id))))

(defn key? [key]
  (isa? (class key) Key))

(defn unpack-key [^Key key]
  (try
    (KeyFactory/keyToString key)
    (catch Exception e nil)))

(defn pack-key [value]
  (try
    (KeyFactory/stringToKey value)
    (catch Exception e
      (throw (IllegalArgumentException. (.getMessage e))))))

(defn- build-native [record]
  (try
    (Entity. (pack-key (:key record)))
    (catch IllegalArgumentException e
      (Entity. (:kind record)))))

(defn pack-entity [entity]
  (let [native (build-native entity)]
    (doseq [[field value] (dissoc entity :kind :key )]
      (.setProperty native (name field) value))
    native))

(defn unpack-entity [native]
  (reduce
    (fn [record entry]
      (assoc record (key entry) (val entry)))
    {:kind (.getKind native) :key (unpack-key (.getKey native))}
    (.getProperties native)))

(defn save-record [service record]
  (let [native (pack-entity record)]
    (.put service native)
    (unpack-entity native)))

(def filter-operators {:= Query$FilterOperator/EQUAL
                       :< Query$FilterOperator/LESS_THAN
                       :<= Query$FilterOperator/LESS_THAN_OR_EQUAL
                       :> Query$FilterOperator/GREATER_THAN
                       :>= Query$FilterOperator/GREATER_THAN_OR_EQUAL
                       :!= Query$FilterOperator/NOT_EQUAL
                       :contains? Query$FilterOperator/IN})

(def sort-directions {:asc Query$SortDirection/ASCENDING
                      :desc Query$SortDirection/DESCENDING})

(defn- -add-filter [query filter]
  (.addFilter
    query
    (name (filter/field filter))
    ((filter/operator filter) filter-operators)
    (filter/value filter)))

(defmulti add-filter (fn [query filter] (filter/operator filter)))

(defn- build-not-equals [filter]
  (filter/make-filter
    :!=
    (filter/field filter)
    (filter/value nil)))

(defmethod add-filter :<= [query filter]
  (-add-filter query filter)
  (-add-filter query (build-not-equals filter)))

(defmethod add-filter :< [query filter]
  (-add-filter query filter)
  (-add-filter query (build-not-equals filter)))

(defmethod add-filter :default [query filter]
  (-add-filter query filter))

(defn- build-query [service kind filters sorts]
  (let [query (Query. (name kind))]
    (doseq [filter filters]
      (add-filter query filter))
    (doseq [sort sorts]
      (.addSort
        query
        (name (sort/field sort))
        ((sort/order sort) sort-directions)))
    (.prepare service query)))

(defn- build-fetch-options [limit offset]
  (let [fetch-options (FetchOptions$Builder/withDefaults)]
    (when limit (.limit fetch-options limit))
    (when offset (.offset fetch-options offset))
    fetch-options))

(defn- find-by-kind [service kind filters sorts limit offset]
  (let [query (build-query service kind filters sorts)
        fetching (build-fetch-options limit offset)]
    (map unpack-entity (iterator-seq (.asQueryResultIterator query fetching)))))

(defn- delete-by-kind [service kind filters]
  (let [query (build-query service kind filters nil)
        records (iterator-seq (.asQueryResultIterator query))
        keys (map #(.getKey %) records)]
    (.delete service keys)))

(defn- count-by-kind [service kind filters]
  (.countEntities (build-query service kind filters nil)))

(defn- find-by-key [service value]
  (try
    (when-let [key (pack-key value)]
      (unpack-entity (.get service key)))
    (catch EntityNotFoundException e
      (log/warn (format "find-by-key error: %s" (.getMessage e)))
      nil)))

(defn- all-kinds [service]
  (let [query (.prepare service (Query. Entities/KIND_METADATA_KIND))
        fetching (build-fetch-options nil nil)]
    (map #(.getName (.getKey %)) (.asIterable query fetching))))

(deftype GaeDatastore [service]
  Datastore
  (ds-save [this records] (doall (map #(save-record service %) records)))
  (ds-delete-by-kind [this kind filters]
    (delete-by-kind service kind filters))
  (ds-delete-by-key [this key]
    (.delete service [(pack-key key)]))
  (ds-count-by-kind [this kind filters] (count-by-kind service kind filters))
  (ds-find-by-key [this key]
    (find-by-key service key))
  (ds-find-by-kind [this kind filters sorts limit offset]
    (find-by-kind service kind filters sorts limit offset))
  (ds-all-kinds [this] (all-kinds service))
  (ds-pack-key [this value] (pack-key value))
  (ds-unpack-key [this kind value] (unpack-key value)))

(defn new-gae-datastore [& args]
  (cond
    (nil? (seq args)) (GaeDatastore. (DatastoreServiceFactory/getDatastoreService))
    (and (= 1 (count args)) (.isInstance DatastoreService (first args))) (GaeDatastore. (first args))
    :else (let [options (->options args)] (GaeDatastore. (or (:service options) (DatastoreServiceFactory/getDatastoreService))))))
