(ns hyperion.query-gen
  (:require
    [clojure.string :as clj-str]))

(defmulti format-table (fn [val] (type val)))
(defmethod format-table java.lang.String [val] val)
(defmethod format-table clojure.lang.Keyword [val] (name val))

(defmulti format-column (fn [val] (type val)))
(defmethod format-column java.lang.String [val] val)
(defmethod format-column clojure.lang.Keyword [val] (name val))
(defmethod format-column clojure.lang.Sequential [val] (str "(" (clj-str/join ", " (map format-column val)) ")"))

(defmulti format-value (fn [val] (type val)))
(defmethod format-value java.lang.String [val] (str "'" val "'"))
(defmethod format-value clojure.lang.Keyword [val] (name val))
(defmethod format-value clojure.lang.Sequential [val] (str "(" (clj-str/join ", " (map format-value val)) ")"))
(defmethod format-value nil [val] "NULL")
(defmethod format-value :default [val] (str val))

(defn build-filter
  ([filter] (build-filter filter (format-value (first filter))))
  ([filter op] (build-filter (format-value (second filter)) op (last filter)))
  ([col op val] (str col " " op " " (format-value val))))

(defmulti filter->sql (fn [filter] (first filter)))

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
  (str name " AS (" query ")"))

(defn add-as [query name]
  (str query " AS " name))

(defn build-subquery [query name]
  (add-as (str "(" query ")") name))

(defn build-insert-query [table item]
  (let [table-name (format-table table)
        column-names (format-value (keys item))
        values (format-value (vals item))]
    (str "INSERT INTO " table-name " " column-names " VALUES " values)))

(defn build-update-query [table item]
  (let [table-name (format-table table)
        filters (map (fn [[col val]] [:= col val]) (dissoc item :id))
        set (clj-str/join ", " (map filter->sql filters))
        query (str "UPDATE " table-name " SET " set)]
    (apply-filters query [[:= :id (:id item)]])))

(defn build-delete-query [table filters]
  (let [table-name (format-table table)
        query (str "DELETE FROM " table-name)]
    (apply-filters query filters)))

(defn build-select-query [table filters sorts limit offset return]
   (->
    (str "SELECT " return " FROM " table)
    (apply-filters filters)
    (apply-sorts sorts)
    (apply-limit limit)
    (apply-offset offset)))

(defn lowest-common-denominator [schema]
  (sort (set (reduce #(concat %1 (second %2)) [] schema))))

(defn seq-contains? [coll item]
  (some #(= % item) coll))

(defn build-return-for-table [table columns lcd-columns]
  [table (clj-str/join ", " (cons (str (format-value table) " AS table_name") (map #(if (seq-contains? columns %) (format-column %) (str "NULL AS " (format-column %))) lcd-columns)))])

(defn build-returns-for-tables [schema]
  (let [lcd-columns (lowest-common-denominator schema)]
    (into
      {}
      (map
        (fn [[table columns]]
          (build-return-for-table table columns lcd-columns))
        schema))))

(defn union-selects [selects]
  (clj-str/join " UNION ALL " (map #(str "(" % ")") selects)))

(defn build-select-all-query [schema filters sorts limit offset return]
  (let [returns (build-returns-for-tables schema)
        selects (map (fn [[table ret]] (build-select-query table filters nil nil nil ret)) returns)
        temp-name "filtered"
        with (str "WITH " temp-name " AS (" (union-selects selects) ")")]
    (str with " " (build-select-query temp-name nil sorts limit offset return))))

(defn build-table-listing-query []
  (build-select-query "information_schema.tables" [[:= :table_schema "public"]] nil nil nil "table_name"))

(defn build-schema-query []
  (build-select-query (str (add-as "information_schema.columns" "columns") ", " (build-subquery (build-table-listing-query) "tables")) [[:= :columns.table_name :tables.table_name]] nil nil nil "tables.table_name, column_name"))

(defprotocol SqlQueryBuilder
  (insert-query [this table item])
  (update-query [this table item])
  (delete-query [this table filters])
  (select-query [this table filters sorts limit offset])
  (select-all-query [this schema filters sorts limit offset])
  (count-query [this table filters])
  (count-all-query [this schema filters])
  (schema-query [this]))

(deftype PostgresQueryBuilder []
  SqlQueryBuilder
  (insert-query [this table item] (build-insert-query table item))
  (update-query [this table item] (build-update-query table item))
  (delete-query [this table filters] (build-delete-query table filters))
  (select-query [this table filters sorts limit offset] (build-select-query table filters sorts limit offset "*"))
  (select-all-query [this schema filters sorts limit offset] (build-select-all-query schema filters sorts limit offset "*"))
  (count-query [this table filters] (build-select-query table filters nil nil nil "COUNT(*)"))
  (count-all-query [this schema filters] (build-select-all-query schema filters nil nil nil "COUNT(*)"))
  (schema-query [this] (build-schema-query)))

(defn new-postgres-query-builder []
  (PostgresQueryBuilder.))
