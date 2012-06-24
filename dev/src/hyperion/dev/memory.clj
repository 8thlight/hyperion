(ns hyperion.dev.memory
  (:use
    [hyperion.core :only [Datastore new?]])
  (:require
    [hyperion.sorting :as sort]
    [hyperion.filtering :as filter]))

(defn- != [a b]
  (not (= a b)))

(defn- create-id []
  (str (java.util.UUID/randomUUID)))

(defn format-kind [kind]
  (if (isa? (type kind) clojure.lang.Keyword)
    (name kind)
    kind))

(defn- save-record [ds record]
  (let [record (if (new? record) (assoc record :id (create-id)) record)
        record (update-in record [:kind] (fn [kind] (format-kind kind)))]
    (dosync
      (alter (.store ds) assoc (:id record) record))
    record))

(defn- find-record-by-key [ds key]
  (get @(.store ds) key))

(defn- delete-records [ds records]
  (dosync
    (apply alter (.store ds) dissoc (map :id records))))

(defn- ->operator [op value]
  (cond
    (= := op) #(= value %)
    (= :!= op) #(not (= value %))
    (= :contains? op) (let [coll (set value)] #(contains? coll %))
    (= :> op) (if (number? value) #(> % value) #(> (.compareTo % value) 0))
    (= :>= op) (if (number? value) #(>= % value) #(>= (.compareTo % value) 0))
    (= :< op) (if (number? value) #(< % value) #(< (.compareTo % value) 0))
    (= :<= op) (if (number? value) #(<= % value) #(<= (.compareTo % value) 0))))

(defn- spec->filter [spec]
  (let [key (filter/field spec)
        value (filter/value spec)
        operator (->operator (filter/operator spec) value)]
    (fn [record] (operator (get record key)))))

(defn- build-filter
  ([kind filter-specs]
    (let [speced-filters (map spec->filter filter-specs)
          all-filters (cons #(= kind (:kind %)) speced-filters)]
      (fn [record] (every? #(% record) all-filters))))
  ([filter-specs]
    (fn [record] (every? #(% record) (map spec->filter filter-specs)))))

(defn- do-query [ds filter-fn sorts limit offset]
  (->> @(.store ds)
    vals
    (filter filter-fn)
    (sort/sort-results sorts)
    (filter/offset-results offset)
    (filter/limit-results limit)))

(defn- find-records-by-kind [ds kind filters sorts limit offset]
  (do-query ds (build-filter kind filters) sorts limit offset))

(defn- find-records [ds filters sorts limit offset]
  (do-query ds (build-filter filters) sorts limit offset))

(defn- all-kinds [ds]
  (set (map (fn [[key record]] (:kind record)) @(.store ds))))

(defn- delete-by-kind [ds kind filters]
  (->> @(.store ds)
    vals
    (filter (build-filter filters))
    (delete-records ds)))

(deftype MemoryDatastore [store]
  Datastore
  (ds-save [this records] (doall (map #(save-record this %) records)))
  (ds-delete-by-id [this kind id]
    (delete-by-kind this kind [(filter/make-filter := :id id)]))
  (ds-delete-by-kind [this kind filters]
    (delete-by-kind this kind filters))
  (ds-count-by-kind [this kind filters] (count (find-records-by-kind this kind filters nil nil nil)))
  (ds-find-by-id [this kind id] (find-record-by-key this id))
  (ds-find-by-kind [this kind filters sorts limit offset] (find-records-by-kind this kind filters sorts limit offset))
  (ds-all-kinds [this] (all-kinds this)))

(defn new-memory-datastore
  ([] (MemoryDatastore. (ref {})))
  ([stuff] (MemoryDatastore. (ref stuff))))
