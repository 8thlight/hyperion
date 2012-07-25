(ns hyperion.mysql
  (:use
    [hyperion.sql.query :only [add-to-query]]
    [hyperion.sql.query-builder]
    [hyperion.sql.format :only [column->db]]
    [hyperion.sql])
  (:require
    [hyperion.sorting :as sort]))

(deftype MysqlQB []
  QueryBuilderStrategy

  (quote-tick [this] "`")

  (apply-limit-and-offset [this query limit offset]
    (if (and (nil? offset) (nil? limit))
      query
      (let [limit (or limit 9223372036854775807)
            offset (or offset 0)]
        (add-to-query query "LIMIT ?, ?" [offset limit])))))

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

(defn new-mysql-datastore [database]
  (new-sql-datastore (MysqlDB. database) (new-query-builder (MysqlQB.))))
