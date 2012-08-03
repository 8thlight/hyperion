(ns hyperion.sqlite
  (:use
    [hyperion.sql.query :only [add-to-query]]
    [hyperion.sql.query-builder]
    [hyperion.sql.format :only [column->db]]
    [hyperion.sql])
  (:require
    [hyperion.sorting :as sort]))

(clojure.lang.RT/loadClassForName "org.sqlite.JDBC")

(deftype SqliteQB []
  QueryBuilderStrategy

  (quote-tick [this] "\"")

  (apply-limit-and-offset [this query limit offset]
    (if (and (nil? offset) (nil? limit))
      query
      (let [limit (or limit 9223372036854775807)
            offset (or offset 0)]
        (add-to-query query "LIMIT ?, ?" [offset limit])))))

(deftype SqliteDB []
  DBStrategy
  (get-count [this result]
    (get result "COUNT(*)"))

  (process-result-record [this result given]
    (if (:id given)
      given
      (assoc given :id (get result "last_insert_rowid()"))))

  (table-listing-query [this]
    "SELECT \"name\" AS \"table_name\" FROM \"sqlite_master\" WHERE \"type\" = 'table'"))

(defn new-sqlite-datastore []
  (new-sql-datastore (SqliteDB.) (new-query-builder (SqliteQB.))))
