(ns hyperion.postgres
  (:require [hyperion.sql.query :refer [add-to-query]]
            [hyperion.sql.query-builder :refer :all ]
            [hyperion.sql.format :refer [column->db]]
            [hyperion.sql :refer :all ]
            [hyperion.sorting :as sort]
            [chee.util :refer [->options]]))

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
      (apply-offset offset)))

  (empty-insert-query [this] "INSERT INTO %s DEFAULT VALUES"))

(deftype PostgresDB []
  DBStrategy
  (get-count [this result]
    (get result "count"))

  (process-insert-result-record [this result given] result)
  (process-update-result-record [this result given id] result)

  (table-listing-query [this]
    "SELECT \"table_name\" FROM \"information_schema\".\"tables\" WHERE \"table_schema\" = 'public'"))

(defn new-postgres-datastore [& args]
  (if (and (= 1 (count args)) (string? (first args)))
    (new-sql-datastore :connection-url (first args) :db (PostgresDB.) :qb (new-query-builder (PostgresQB.)))
    (let [options (->options args)]
      (new-sql-datastore options :db (PostgresDB.) :qb (new-query-builder (PostgresQB.))))))
