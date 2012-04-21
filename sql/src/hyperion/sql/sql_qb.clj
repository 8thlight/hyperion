(ns hyperion.sql.sql-qb
  (:require
    [clojure.string :as clj-str]))

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
      (str (type-cast-fn (format-value value) type) " AS " (format-column name)))
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

(def sql-query-builder-fns {:insert build-insert
                           :update build-update
                           :delete build-delete
                           :select select
                           :select-all select-all
                           :count-all count-all
                           :union-all build-union-all
                           :column-listing column-listing})
