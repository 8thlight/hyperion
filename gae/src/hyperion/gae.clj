(ns hyperion.gae
  (:use
    [chee.string :only (gsub spear-case)]
    [chee.util :only (->options)]
    [hyperion.core :only (Datastore *entity-specs*)])
  (:require
    [clojure.string :as str]
    [hyperion.gae.types]
    [hyperion.sorting :as sort]
    [hyperion.filtering :as filter])
  (:import
    [com.google.appengine.api.datastore Entity Entities Query DatastoreServiceFactory Query$FilterOperator
     Query$SortDirection FetchOptions$Builder EntityNotFoundException KeyFactory Key]))

(defn create-key [kind id]
  (if (number? id)
    (KeyFactory/createKey kind (long id))
    (KeyFactory/createKey kind (str id))))

(defn key? [key]
  (isa? (class key) Key))

(defn key->string [^Key key]
  (try
    (KeyFactory/keyToString key)
    (catch Exception e nil)))

(defn string->key [value]
  (try
    (KeyFactory/stringToKey value)
    (catch Exception e nil)))

(defn ->key [value]
  (cond
    (key? value) value
    (string? value) (string->key value)
    (nil? value) nil
    :else (string->key (:key value))))

(defn ->native [entity]
  (let [key (string->key (:key entity))
        native (if key (Entity. key) (Entity. (:kind entity)))]
    (doseq [[field value] (dissoc entity :kind :key)]
      (.setProperty native (name field) value))
    native))

(defn <-native [native]
  (reduce
    (fn [record entry]
      (assoc record (key entry) (val entry)))
    {:kind (.getKind native) :key (key->string (.getKey native))}
    (.getProperties native)))

(defn save-record [service record]
  (let [native (->native record)]
    (.put service native)
    (<-native native)))

(def filter-operators {:= Query$FilterOperator/EQUAL
                       :< Query$FilterOperator/LESS_THAN
                       :<= Query$FilterOperator/LESS_THAN_OR_EQUAL
                       :> Query$FilterOperator/GREATER_THAN
                       :>= Query$FilterOperator/GREATER_THAN_OR_EQUAL
                       :!= Query$FilterOperator/NOT_EQUAL
                       :contains? Query$FilterOperator/IN})

(def sort-directions {:asc Query$SortDirection/ASCENDING
                      :desc Query$SortDirection/DESCENDING})

(defn- build-query [service kind filters sorts]
  (let [query (Query. (name kind))]
    (doseq [filter filters]
      (.addFilter
        query
        (name (filter/field filter))
        ((filter/operator filter) filter-operators)
        (filter/value filter)))
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
    (map <-native (iterator-seq (.asQueryResultIterator query fetching)))))

(defn- delete-records [service keys]
  (.delete service (map ->key keys)))

(defn- delete-by-kind [service kind filters]
  (let [query (build-query service kind filters nil)
        records (iterator-seq (.asQueryResultIterator query))
        keys (map #(.getKey %) records)]
    (.delete service keys)))

(defn- count-by-kind [service kind filters]
  (.countEntities (build-query service kind filters nil)))

(defn- find-by-key [service value]
  (when-let [key (->key value)]
    (try
      (<-native (.get service key))
      (catch EntityNotFoundException e
        nil))))

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
    (.delete service [(->key key)]))
  (ds-count-by-kind [this kind filters] (count-by-kind service kind filters))
  (ds-find-by-key [this key]
    (find-by-key service key))
  (ds-find-by-kind [this kind filters sorts limit offset]
    (find-by-kind service kind filters sorts limit offset))
  (ds-all-kinds [this] (all-kinds service)))

(defn new-gae-datastore
  ([] (GaeDatastore. (DatastoreServiceFactory/getDatastoreService)))
  ([service] (GaeDatastore. service)))
