(ns hyperion.sqlite
  (:require
    [hyperion.sql.query :refer [add-to-query]]
    [hyperion.sql.query-builder :refer :all]
    [hyperion.sql.format :refer [column->db]]
    [hyperion.sql :refer :all]
    [hyperion.sorting :as sort]
    [chee.util :refer [->options]]))

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

  (process-result-record [this result given]
    (if (:id given)
      given
      (assoc given :id (get result "last_insert_rowid()"))))

  (table-listing-query [this]
    "SELECT \"name\" AS \"table_name\" FROM \"sqlite_master\" WHERE \"type\" = 'table'"))

(defn new-sqlite-datastore [& args]
  (if (and (= 1 (count args)) (string? (first args)))
    (new-sql-datastore :connection-url (first args) :db (SqliteDB.) :qb (new-query-builder (SqliteQB.)))
    (let [options (->options args)]
      (new-sql-datastore options :db (SqliteDB.) :qb (new-query-builder (SqliteQB.))))))
