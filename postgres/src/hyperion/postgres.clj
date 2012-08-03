(ns hyperion.postgres
  (:use
    [hyperion.sql.query :only [add-to-query]]
    [hyperion.sql.query-builder]
    [hyperion.sql.format :only [column->db]]
    [hyperion.sql])
  (:require
    [hyperion.sorting :as sort]))

(clojure.lang.RT/loadClassForName "org.postgresql.Driver")

(defn- apply-limit [query limit]
  (if (nil? limit)
    query
    (add-to-query query "LIMIT ?" [limit])))

(defn- apply-offset [query offset]
  (if (nil? offset)
    query
    (add-to-query query "OFFSET ?" [offset])))

(deftype PostgresQB []
  QueryBuilderStrategy

  (quote-tick [this] "\"")

  (apply-limit-and-offset [this query limit offset]
    (-> query
      (apply-limit limit)
      (apply-offset offset))))

(deftype PostgresDB []
  DBStrategy
  (get-count [this result]
    (get result "count"))

  (process-result-record [this result given] result)

  (table-listing-query [this]
    "SELECT \"table_name\" FROM \"information_schema\".\"tables\" WHERE \"table_schema\" = 'public'"))

(defn new-postgres-datastore []
  (new-sql-datastore (PostgresDB.) (new-query-builder (PostgresQB.))))
