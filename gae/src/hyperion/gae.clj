(ns hyperion.gae
  (:use
    [chee.string :only (gsub spear-case)]
    [chee.util :only (->options)]
    [hyperion.core :only (Datastore *entity-specs* pack-field)])
  (:require
    [clojure.string :as str]
    [hyperion.gae.types])
  (:import
    [com.google.appengine.api.datastore Entity Query DatastoreServiceFactory Query$FilterOperator
     Query$SortDirection FetchOptions$Builder EntityNotFoundException KeyFactory Key]))

(defn ->kind [thing]
  (if (isa? (class thing) Entity)
    (.getKind thing)
    nil))

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
    (fn [entity entry] (assoc entity (keyword (key entry)) (val entry)))
    {:kind (.getKind native) :key (key->string (.getKey native))}
    (.getProperties native)))

(defn save-native [service native]
  (.put service native)
  native)

(defn delete-records [service keys]
  (.delete service (map ->key keys)))


(def filter-operators {:= Query$FilterOperator/EQUAL
                       :< Query$FilterOperator/LESS_THAN
                       :<= Query$FilterOperator/LESS_THAN_OR_EQUAL
                       :> Query$FilterOperator/GREATER_THAN
                       :>= Query$FilterOperator/GREATER_THAN_OR_EQUAL
                       :!= Query$FilterOperator/NOT_EQUAL
                       :contains? Query$FilterOperator/IN})

(def sort-directions {:asc Query$SortDirection/ASCENDING
                      :desc Query$SortDirection/DESCENDING})

(defn build-query [service kind filters sorts options]
  (let [spec (get @*entity-specs* kind)]
    (let [query (if kind (Query. (name kind)) (Query.))]
      (doseq [[operator field value] filters]
        (.addFilter query (name field) (get filter-operators operator)
          (pack-field (:packer (get spec field)) value)))
      (doseq [[field direction] sorts]
        (.addSort query (name field) (get sort-directions direction)))
      (.prepare service query))))

(defn build-fetch-options [limit offset options]
  (let [prefetch-size (:prefetch-size options)
        chunk-size (:chunk-size options)
        start-cursor (:start-cursor options)
        end-cursor (:end-cursor options)]
    (let [fetch-options (FetchOptions$Builder/withDefaults)]
      (when limit (.limit fetch-options limit))
      (when offset (.offset fetch-options offset))
      (when prefetch-size (.prefetchSize fetch-options prefetch-size))
      (when chunk-size (.checkSize fetch-options chunk-size))
      (when start-cursor (.startCursor fetch-options start-cursor))
      (when end-cursor (.endCursor fetch-options end-cursor))
      fetch-options)))

(defn find-by-kind [service kind filters sorts limit offset options]
  (let [query (build-query service kind filters sorts options)
        fetching (build-fetch-options limit offset options)]
    (let [results (.asQueryResultIterator query fetching)]
      (iterator-seq results))))

(defn count-by-kind [service kind filters options]
  (.countEntities
    (build-query service kind filters nil options)
    (build-fetch-options nil nil options)))

(defn find-by-key [service value]
  (when-let [key (->key value)]
    (try
      (.get service key)
      (catch EntityNotFoundException e
        nil))))

(defn find-by-keys [service ^Iterable keys]
  (let [keys (filter identity (map ->key keys))
        result-map (.get service keys)]
    (map #(get result-map %) keys)))

(deftype GaeDatastore [service]
  Datastore
  (ds->kind [this thing] (->kind thing))
  (ds->ds-key [this thing] (->key thing))
  (ds->string-key [this thing] (string->key thing))
  (ds-save [this native] (save-native service native))
  (ds-save* [this natives] (doall (for [native natives] (save-native service native))))
  (ds-delete [this keys] (delete-records service keys))
  (ds-count-by-kind [this kind filters options] (count-by-kind service kind filters options))
  (ds-count-all-kinds [this filters options] (count-by-kind service nil filters options))
  (ds-find-by-key [this key] (find-by-key service key))
  (ds-find-by-keys [this keys] (find-by-keys service keys))
  (ds-find-by-kind [this kind filters sorts limit offset options]
    (find-by-kind service kind filters sorts limit offset options))
  (ds-find-all-kinds [this filters sorts limit offset options]
    (find-by-kind service nil filters sorts limit offset options))
  (ds-native->entity [this native] (<-native native))
  (ds-entity->native [this entity] (->native entity)))

(defn new-gae-datastore
  ([] (GaeDatastore. (DatastoreServiceFactory/getDatastoreService)))
  ([service] (GaeDatastore. service)))