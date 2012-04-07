(ns hyperion.sql.datastore
  (:use
    [hyperion.core :only [Datastore new?]]
    [hyperion.sql.query-builder]
    [hyperion.sql.query-executor])
  (:require
    [clojure.string :as clj-str]
    [clojure.set :as clj-set]))

(defmulti format-table type)
(defmethod format-table clojure.lang.Keyword [table] (name table))
(defmethod format-table :default [table] table)

(defn build-key [table-name id]
  (str (format-table table-name) "-" id))

(defn destructure-key [key]
  (let [index (.lastIndexOf key "-")
        table-name (.substring key 0 index)
        id (Integer/parseInt (.substring key (inc index) (.length key)))]
    [table-name id]))

(defn apply-kind-and-key
  ([record] (apply-kind-and-key record (:kind record) (:id record)))
  ([record table-name](apply-kind-and-key record table-name (:id record)))
  ([record table-name id]
    (assoc record :kind table-name :key (build-key table-name id))))

(defn insert-record [query-builder record]
  (let [table-name (:kind record)
        record (dissoc record :kind)
        query (insert query-builder table-name record)]
    [table-name query]))

(defn update-record [query-builder record]
  (let [[table-name id] (destructure-key (:key record))
        record (assoc record :id id)
        record (dissoc record :kind :key)
        query (update query-builder table-name record)]
    [table-name query]))

(defn save-record [ds record]
  (let [[table-name query]
          ((if (new? record)
            insert-record
            update-record) (.query-builder ds) record)
        result (do-command (.query-executor ds) query)]
    (apply-kind-and-key result table-name)))

(defn save-records [ds records]
  (doall (map #(save-record ds %) records)))

(defn delete-record [ds table-name id]
  (let [query (delete (.query-builder ds) table-name [[:= :id id]])]
    (do-command (.query-executor ds) query)))

(defn delete-records [ds keys]
  (doseq [key keys]
    (let [[table-name id] (destructure-key key)]
      (delete-record ds table-name id))))

(defn count-records-by-kind [ds kind filters]
  (let [query (count-all (.query-builder ds) nil kind filters)
        results (do-query (.query-executor ds) query)]
    (:count (first results))))

(defn find-records-by-kind [ds kind filters sorts limit offset]
  (let [query (select-all (.query-builder ds) nil kind filters sorts limit offset)
        results (do-query (.query-executor ds) query)]
    (map #(apply-kind-and-key % kind) results)))

(defn find-record-by-key [ds key]
  (let [[table-name id] (destructure-key key)
        records (find-records-by-kind ds table-name [[:= :id id]] nil nil nil)]
    (first records)))

(defn parse-column-listing [column-listing]
  (let [schema (reduce #(assoc %1 (keyword (:table_name %2)) []) {} column-listing)]
    (loop [[{:keys [table_name column_name data_type]} & more] column-listing schema schema dist-cols (sorted-set)]
      (if (nil? table_name)
        [schema dist-cols]
        (let [table (keyword table_name)
              col [(keyword column_name) (keyword data_type)]
              schema (update-in schema [table] #(conj % col))
              dist-cols (conj dist-cols col)]
          (recur more schema dist-cols))))))

(defn get-schema-and-distinct-columns [ds]
  (let [column-listing-query (column-listing (.query-builder ds))
        results (do-query (.query-executor ds) column-listing-query)]
    (parse-column-listing results)))

(defn seq-contains? [coll item]
  (some #(= % item) coll))

(defn- build-padded-returns [table-name cols dist-cols]
  (let [diff (clj-set/difference (set dist-cols) (set cols))]
    (cons
      [(name table-name) :table_name :text]
      (map #(if (seq-contains? diff %) (cons nil %) (first %)) dist-cols))))

(defn- build-padded-select [query-builder table cols dist-cols filters]
  (let [returns (build-padded-returns table cols dist-cols)]
    (select query-builder nil returns table filters nil nil nil)))

(defn- build-filtered-union-by-all-kinds [qb schema dist-cols filters]
  (let [table-select-queries (map (fn [[table cols]] (build-padded-select qb table cols dist-cols filters)) schema)]
    (union-all qb table-select-queries)))

(defn col-names [cols]
  (map first cols))

(defn clean-padding-and-apply-keys [record schema]
  (let [table-name (keyword (:table_name record))
        record (select-keys record (col-names (table-name schema)))]
    (apply-kind-and-key record table-name)))

(defn find-records-by-all-kinds [ds filters sorts limit offset]
  (let [qb (.query-builder ds)
        [schema dist-cols] (get-schema-and-distinct-columns ds)
        filtered-union (build-filtered-union-by-all-kinds qb schema dist-cols filters)
        with-name "filtered"
        query (select-all qb [[with-name filtered-union]] with-name nil sorts limit offset)
        results (do-query (.query-executor ds) query)]
    (map #(clean-padding-and-apply-keys % schema) results)))

(defn count-records-by-all-kinds [ds filters]
  (let [qb (.query-builder ds)
        [schema dist-cols] (get-schema-and-distinct-columns ds)
        filtered-union (build-filtered-union-by-all-kinds qb schema dist-cols filters)
        with-name "filtered"
        query (count-all qb [[with-name filtered-union]] with-name nil)
        results (do-query (.query-executor ds) query)]
    (:count (first results))))

(deftype SqlDatastore [query-executor query-builder]
  Datastore
  (ds-save [this record] (save-record this record))
  (ds-save* [this records] (save-records this records))
  (ds-delete [this keys] (delete-records this keys))
  (ds-count-by-kind [this kind filters] (count-records-by-kind this kind filters))
  (ds-count-all-kinds [this filters] (count-records-by-all-kinds this filters))
  (ds-find-by-key [this key] (find-record-by-key this key))
  (ds-find-by-kind [this kind filters sorts limit offset] (find-records-by-kind this kind filters sorts limit offset))
  (ds-find-all-kinds [this filters sorts limit offset] (find-records-by-all-kinds this filters sorts limit offset)))

(defn new-sql-datastore [query-executor query-builder]
  (SqlDatastore. query-executor query-builder))
