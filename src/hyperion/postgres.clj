(ns hyperion.postgres
  (:use
    [hyperion.core]
    [hyperion.query-gen])
  (:require
    [clojure.java.jdbc :as sql]
    [clojure.java.jdbc.internal :as sql-internal]
    [clojure.string :as clj-str]))

(defn do-query [conn query]
  (sql/with-connection conn
    (sql/with-query-results
      res [query]
      (doall res))))

(defn do-command [conn command]
  (sql/with-connection conn
    (sql-internal/do-prepared-return-keys* command nil)))

(defn build-key [table-name id]
  (str table-name "-" id))

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
        query (insert-query query-builder table-name record)]
    [table-name query]))

(defn update-record [query-builder record]
  (let [[table-name id] (destructure-key (:key record))
        record (assoc record :id id)
        record (dissoc record :kind :key)
        query (update-query query-builder table-name record)]
    [table-name query]))

(defn save-record [ds record]
  (let [[table-name query]
          ((if (new? record)
            insert-record
            update-record) (.query-builder ds) record)
        result (do-command (.conn ds) query)]
    (apply-kind-and-key result table-name)))

(defn save-records [ds records]
  (doall (map #(save-record ds %) records)))

(defn delete-record [ds table-name id]
  (let [query (delete-query (.query-builder ds) table-name [[:= :id id]])]
    (do-command (.conn ds) query)))

(defn delete-records [ds keys]
  (doseq [key keys]
    (let [[table-name id] (destructure-key key)]
      (delete-record ds table-name id))))

(defn count-records-by-kind [ds kind filters]
  (let [query (count-query (.query-builder ds) kind filters)
        results (do-query (.conn ds) query)]
    (:count (first results))))

(defn count-records-by-all-kinds [ds filters]
  (let [query (count-all-query (.query-builder ds) (.schema ds) filters)
        results (do-query (.conn ds) query)]
    (:count (first results))))

(defn find-records-by-kind [ds kind filters sorts limit offset]
  (let [query (select-query (.query-builder ds) kind filters sorts limit offset)
        results (do-query (.conn ds) query)]
    (map #(apply-kind-and-key % kind) results)))

(defn find-record-by-key [ds key]
  (let [[table-name id] (destructure-key key)
        records (find-records-by-kind ds table-name [[:= :id id]] nil nil nil)]
    (first records)))

(defn clean-record [record schema]
  (let [table-name (:table_name record)
        record (select-keys record (get schema table-name))]
    (apply-kind-and-key record table-name)))

(defn find-records-by-all-kinds [ds filters sorts limit offset]
  (let [schema (.schema ds)
        query (select-all-query (.query-builder ds) schema filters sorts limit offset)
        results (do-query (.conn ds) query)]
    (map #(clean-record % schema) results)))

(deftype SqlDatastore [conn schema query-builder]
  Datastore
  (ds-save [this record] (save-record this record))
  (ds-save* [this records] (save-records this records))
  (ds-delete [this keys] (delete-records this keys))
  (ds-count-by-kind [this kind filters] (count-records-by-kind this kind filters))
  (ds-count-all-kinds [this filters] (count-records-by-all-kinds this filters))
  (ds-find-by-key [this key] (find-record-by-key this key))
  (ds-find-by-kind [this kind filters sorts limit offset] (find-records-by-kind this kind filters sorts limit offset))
  (ds-find-all-kinds [this filters sorts limit offset] (find-records-by-all-kinds this filters sorts limit offset)))

(defn- build-schema [columns]
  (let [schema (reduce #(assoc %1 (:table_name %2) []) {} columns)]
    (reduce
      (fn [schema {:keys [table_name column_name]}]
        (update-in schema [table_name] #(conj % (keyword column_name))))
        schema
        columns)))

(defn new-postgres-datastore [conn]
  (let [query-gen (new-postgres-query-builder)
        schema-query (schema-query query-gen)
        results (do-query conn schema-query)
        schema (build-schema results)]
  (SqlDatastore. conn schema query-gen)))
