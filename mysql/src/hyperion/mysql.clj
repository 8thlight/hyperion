(ns hyperion.mysql
  (:require [hyperion.sorting :as sort]
            [hyperion.sql.query :refer [add-to-query]]
            [hyperion.sql.query-builder :refer :all ]
            [hyperion.sql.format :refer [column->db]]
            [hyperion.sql :refer :all ]
            [chee.util :refer [->options]]))

(clojure.lang.RT/loadClassForName "com.mysql.jdbc.Driver")

(deftype MysqlQB []
  QueryBuilderStrategy

  (quote-tick [this] "`")

  (apply-limit-and-offset [this query limit offset]
    (if (and (nil? offset) (nil? limit))
      query
      (let [limit (or limit 9223372036854775807)
            offset (or offset 0)]
        (add-to-query query "LIMIT ?, ?" [offset limit]))))

  (empty-insert-query [this] "INSERT INTO %s () VALUES ()"))

(deftype MysqlDB [database]
  DBStrategy
  (get-count [this result]
    (get result "COUNT(*)"))

  (process-result-record [this result given]
    (if (:id given)
      given
      (assoc given :id (get result "GENERATED_KEY"))))

  (table-listing-query [this]
    (format "SELECT `table_name` FROM `information_schema`.`tables` WHERE `table_schema` = '%s'" database)))

(defn new-mysql-datastore [& args]
  (let [options (->options args)]
    (new-sql-datastore options :db (MysqlDB. (:database options)) :qb (new-query-builder (MysqlQB.)))))
