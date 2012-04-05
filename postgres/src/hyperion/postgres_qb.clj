(ns hyperion.postgres-qb
  (:use
    [hyperion.sql.query-builder])
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
  (str name " AS (" query  ")"))

(defn build-withs [withs]
  (when-not (empty? withs)
    (str "WITH " (clj-str/join ", " (map build-with withs)) " ")))

(defn build-return-statement [returns]
  (clj-str/join ", " (map
    (fn [return]
      (if (coll? return)
        (str (format-value (first return)) " AS " (format-value (second return)))
        (format-value return)))
    returns)))

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

(defn build-select [withs return-statement table filters sorts limit offset]
   (->
    (str (build-withs withs) "SELECT " return-statement " FROM " (format-table table))
    (apply-filters filters)
    (apply-sorts sorts)
    (apply-limit limit)
    (apply-offset offset)))

(defn build-union-all [queries]
  (clj-str/join " UNION ALL " (map #(str "(" % ")") queries)))

(defn build-subquery [query name]
  (str "(" query ") AS " name))

(defn build-table-listing []
  (build-select nil "table_name" "information_schema.tables" [[:= :table_schema "public"]] nil nil nil))

(defn build-column-listing []
  (build-select nil "tables.table_name, column_name" (str "information_schema.columns AS columns, " (build-subquery (build-table-listing) "tables")) [[:= :columns.table_name :tables.table_name]] nil nil nil))

(deftype PostgresQueryBuilder []
  QueryBuilder
  (insert [this table item] (build-insert table item))
  (update [this table item] (build-update table item))
  (delete [this table filters] (build-delete table filters))
  (select [this withs returns table filters sorts limit offset] (build-select withs (build-return-statement returns) table filters sorts limit offset))
  (select-all [this withs table filters sorts limit offset] (build-select withs "*" table filters sorts limit offset))
  (count-all [this withs table filters] (build-select withs "COUNT(*)" table filters nil nil nil))
  (union-all [this queries] (build-union-all queries))
  (column-listing [this] (build-column-listing)))

(defn new-postgres-query-builder []
  (PostgresQueryBuilder.))
