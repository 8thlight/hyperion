(ns hyperion.postgres
  (:require
    [clojure.java.jdbc :as sql]
    [clojure.string :as clj-str]
    [clojure.set :as clj-set]
    [hyperion.core :refer [Datastore new?]]
    [clojure.java.jdbc.internal :as sql-internal]))

(defmulti format-table type)
(defmethod format-table java.lang.String [val] val)
(defmethod format-table clojure.lang.Keyword [val] (name val))

(defmulti format-column type)
(defmethod format-column java.lang.String [val] val)
(defmethod format-column clojure.lang.Keyword [val] (name val))
(defmethod format-column clojure.lang.Sequential [val] (str "(" (clj-str/join ", " (map format-column val)) ")"))

(defmulti format-value type)
(defmethod format-value java.lang.String [val] (str "'" val "'"))
(defmethod format-value clojure.lang.Keyword [val] (name val))
(defmethod format-value clojure.lang.Sequential [val] (str "(" (clj-str/join ", " (map format-value val)) ")"))
(defmethod format-value java.util.Date [val] (format-value (str val)))
(defmethod format-value nil [val] "NULL")
(defmethod format-value :default [val] (str val))

(defn build-filter
  ([filter] (build-filter filter (format-value (first filter))))
  ([filter op] (build-filter (format-value (second filter)) op (last filter)))
  ([col op val] (str col " " op " " (format-value val))))

(defn- format-type [pg-type]
  (if (isa? (type pg-type) clojure.lang.Keyword)
    (name pg-type)
    pg-type))

(defn- type-cast [value type]
  (if (nil? type)
    value
    (str value "::" (format-type type))))

(defmulti filter->sql first)

(defmethod filter->sql :!= [filter]
  (build-filter filter "<>"))

(defmethod filter->sql :contains? [filter]
  (build-filter filter "IN"))

(defmethod filter->sql :default [filter]
  (build-filter filter))

(defn apply-filters [query filters]
  (if (empty? filters)
    query
    (let [where-clause (str "WHERE " (clj-str/join " AND " (map filter->sql filters)))]
      (str query " " where-clause))))

(defn sort->sql [sort]
  (let [order (case (second sort) :asc "ASC" :desc "DESC")]
    (str (format-value (first sort)) " " order)))

(defn apply-sorts [query sorts]
  (if (empty? sorts)
    query
    (let [order-by-clause (str "ORDER BY " (clj-str/join ", " (map sort->sql sorts)))]
      (str query " " order-by-clause))))

(defn apply-limit [query limit]
  (if (nil? limit)
    query
    (str query " LIMIT " limit)))

(defn apply-offset [query offset]
  (if (nil? offset)
    query
    (str query " OFFSET " offset)))

(defn build-with [[name query]]
  (str (format-table name) " AS (" query  ")"))

(defn build-withs [withs]
  (when-not (empty? withs)
    (str "WITH " (clj-str/join ", " (map build-with withs)) " ")))

(defn build-return [return type-cast-fn]
  (if (coll? return)
    (let [[value name type] return]
      (str (type-cast-fn (format-value value) type) " AS " (format-table name)))
    (format-value return)))

(defn build-return-statement [returns type-cast-fn]
  (clj-str/join ", " (map #(build-return % type-cast-fn) returns)))

(defn build-insert [table item]
  (let [table-name (format-table table)
        column-names (format-value (keys item))
        values (format-value (vals item))]
    (str "INSERT INTO " table-name " " column-names " VALUES " values)))

(defn build-update [table item]
  (let [table-name (format-table table)
        filters (map (fn [[col val]] [:= col val]) (dissoc item :id))
        set (clj-str/join ", " (map filter->sql filters))
        query (str "UPDATE " table-name " SET " set)]
    (apply-filters query [[:= :id (:id item)]])))

(defn build-delete [table filters]
  (let [table-name (format-table table)
        query (str "DELETE FROM " table-name)]
    (apply-filters query filters)))

(defn- build-select [withs return-statement table filters sorts limit offset]
   (->
    (str (build-withs withs) "SELECT " return-statement " FROM " (format-table table))
    (apply-filters filters)
    (apply-sorts sorts)
    (apply-limit limit)
    (apply-offset offset)))

(defn select [withs returns table filters sorts limit offset type-cast-fn]
  (build-select withs (build-return-statement returns type-cast-fn) table filters sorts limit offset))

(defn select-all [withs table filters sorts limit offset]
  (build-select withs "*" table filters sorts limit offset))

(defn count-all [withs table filters]
  (build-select withs "COUNT(*)" table filters nil nil nil))

(defn build-union-all [queries]
  (clj-str/join " UNION ALL " (map #(str "(" % ")") queries)))

(defn- build-subquery [query name]
  (str "(" query ") AS " name))

(defn- build-table-listing [database select-fn]
  (select-fn nil [:table_name] :information_schema.tables [[:= :table_schema database]] nil nil nil))

(defn column-listing [database select-fn]
  (select-fn nil [:tables.table_name :column_name :data_type] (str "information_schema.columns AS columns, " (build-subquery (build-table-listing database select-fn) "tables")) [[:= :columns.table_name :tables.table_name]] nil nil nil))


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

(defn insert-record [record]
  (let [table-name (format-table (:kind record))
        record (dissoc record :kind)
        query (build-insert table-name record)]
    [table-name query]))

(defn update-record [record]
  (let [[table-name id] (destructure-key (:key record))
        record (assoc record :id id)
        record (dissoc record :kind :key)
        query (build-update table-name record)]
    [table-name query]))

(defn save-record [record]
  (let [[table-name query]
          ((if (new? record)
            insert-record
            update-record) record)
        result (sql-internal/do-prepared-return-keys* query nil)]
    (apply-kind-and-key result table-name)))

(defn save-records [records]
  (doall (map #(save-record %) records)))

(defn delete-record [table-name id]
  (let [query (build-delete table-name [[:= :id id]])]
    (sql-internal/do-prepared-return-keys* query nil)))

(defn delete-records [keys]
  (doseq [key keys]
    (let [[table-name id] (destructure-key key)]
      (delete-record table-name id))))

(defn count-records-by-kind [kind filters]
  (let [query (count-all nil kind filters)]
    (sql/with-query-results
      results [query]
      (:count (first results)))))

(defn find-records-by-kind [kind filters sorts limit offset]
  (let [query (select-all nil kind filters sorts limit offset)]
    (sql/with-query-results
      results [query]
      (doall (map #(apply-kind-and-key % kind) results)))))

(defn find-record-by-key [key]
  (let [[table-name id] (destructure-key key)
        records (find-records-by-kind table-name [[:= :id id]] nil nil nil)]
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
  (let [column-listing-query (column-listing)]
    (sql/with-query-results
      results [column-listing-query]
      (parse-column-listing results))))

(defn seq-contains? [coll item]
  (some #(= % item) coll))

(defn- build-padded-returns [table-name cols dist-cols]
  (let [diff (clj-set/difference (set dist-cols) (set cols))]
    (cons
      [(name table-name) :table_name :text]
      (map #(if (seq-contains? diff %) (cons nil %) (first %)) dist-cols))))

(defn- build-padded-select [table cols dist-cols filters]
  (let [returns (build-padded-returns table cols dist-cols)]
    (select nil returns table filters nil nil nil)))

(defn- build-filtered-union-by-all-kinds [schema dist-cols filters]
  (let [table-select-queries (map (fn [[table cols]] (build-padded-select table cols dist-cols filters)) schema)]
    (build-union-all table-select-queries)))

(defn col-names [cols]
  (map first cols))

(defn clean-padding-and-apply-keys [record schema]
  (let [table-name (keyword (:table_name record))
        record (select-keys record (col-names (table-name schema)))]
    (apply-kind-and-key record table-name)))

(defn find-records-by-all-kinds [ds filters sorts limit offset]
  (let [[schema dist-cols] (get-schema-and-distinct-columns)
        filtered-union (build-filtered-union-by-all-kinds schema dist-cols filters)
        with-name "filtered"
        query (select-all [[with-name filtered-union]] with-name nil sorts limit offset)]
    (sql/with-query-results
      results [query]
      (doall (map #(clean-padding-and-apply-keys % schema) results)))))

(defn count-records-by-all-kinds [filters]
  (let [[schema dist-cols] (get-schema-and-distinct-columns)
        filtered-union (build-filtered-union-by-all-kinds schema dist-cols filters)
        with-name "filtered"
        query (count-all [[with-name filtered-union]] with-name nil)]
    (sql/with-query-results
      results [query]
      (:count (first results)))))

(deftype PostgresDatastore [database]
  Datastore
  (ds->kind [this thing] (if (string? thing) thing nil))
  (ds->ds-key [this thing] (if (string? thing) thing nil))
  (ds->string-key [this thing] thing)
  (ds-save [this record] (save-record record))
  (ds-save* [this records] (save-records records))
  (ds-delete [this keys] (delete-records keys))
  (ds-count-by-kind [this kind filters options] (count-records-by-kind kind filters))
  (ds-count-all-kinds [this filters options] (count-records-by-all-kinds filters))
  (ds-find-by-key [this key] (find-record-by-key key))
  (ds-find-by-kind [this kind filters sorts limit offset options] (find-records-by-kind kind filters sorts limit offset))
  (ds-find-all-kinds [this filters sorts limit offset options] (find-records-by-all-kinds filters sorts limit offset))
  (ds-native->entity [this entity] entity)
  (ds-entity->native [this map] map))

(defn new-postgres-datastore [database]
  (PostgresDatastore. database))
