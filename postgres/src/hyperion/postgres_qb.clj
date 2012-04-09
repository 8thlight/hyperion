(ns hyperion.postgres-qb
  (:use
    [hyperion.sql.query-builder-fn :only [new-query-builder-fn]]
    [hyperion.sql.sql-qb :only [sql-query-builder-fns]]))

(defn format-type [pg-type]
  (if (isa? (type pg-type) clojure.lang.Keyword)
    (name pg-type)
    pg-type))

(defn- build-subquery [query name]
  (str "(" query ") AS " name))

(defn- build-table-listing [select-fn]
  (select-fn nil [:table_name] :information_schema.tables [[:= :table_schema "public"]] nil nil nil))

(defn column-listing [select-fn]
  (select-fn nil [:tables.table_name :column_name :data_type] (str "information_schema.columns AS columns, " (build-subquery (build-table-listing select-fn) "tables")) [[:= :columns.table_name :tables.table_name]] nil nil nil))

(defn type-cast [value type]
  (if (nil? type)
    value
    (str value "::" (format-type type))))

(def pg-query-builder-fns (merge sql-query-builder-fns {:column-listing column-listing
                                                        :type-cast type-cast}))

(defn new-postgres-query-builder []
  (new-query-builder-fn pg-query-builder-fns))
