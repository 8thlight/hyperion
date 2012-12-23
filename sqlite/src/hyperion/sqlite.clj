(ns hyperion.sqlite
  (:require [chee.util :refer [->options]]
            [hyperion.sorting :as sort]
            [hyperion.sql.query :refer [add-to-query]]
            [hyperion.sql.query-builder :refer :all]
            [hyperion.sql.format :refer [column->db]]
            [hyperion.sql.types]
            [hyperion.sql :refer :all]
            [hyperion.sqlite.types]))

(clojure.lang.RT/loadClassForName "org.sqlite.JDBC")

(deftype SqliteQB []
  QueryBuilderStrategy

  (quote-tick [this] "\"")

  (apply-limit-and-offset [this query limit offset]
    (if (and (nil? offset) (nil? limit))
      query
      (let [limit (or limit 9223372036854775807)
            offset (or offset 0)]
        (add-to-query query "LIMIT ?, ?" [offset limit]))))

  (empty-insert-query [this] "INSERT INTO %s DEFAULT VALUES"))

(deftype SqliteDB []
  DBStrategy
  (get-count [this result]
    (get result "COUNT(*)"))

  (process-insert-result-record [this result given]
    (assoc given :id (get result "last_insert_rowid()")))

  (process-update-result-record [this result given id]
    (assoc given :id id))

  (table-listing-query [this]
    "SELECT \"name\" AS \"table_name\" FROM \"sqlite_master\" WHERE \"type\" = 'table'"))

(defn new-sqlite-datastore [& args]
  (if (and (= 1 (count args)) (string? (first args)))
    (new-sql-datastore :connection-url (first args) :db (SqliteDB.) :qb (new-query-builder (SqliteQB.)))
    (let [options (->options args)]
      (new-sql-datastore options :db (SqliteDB.) :qb (new-query-builder (SqliteQB.))))))
