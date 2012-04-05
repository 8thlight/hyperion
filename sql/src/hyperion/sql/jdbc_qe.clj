(ns hyperion.sql.jdbc-qe
  (:use
    [hyperion.sql.query-executor])
  (:require
    [clojure.java.jdbc :as sql]
    [clojure.java.jdbc.internal :as sql-internal]))

(defn -do-query [conn query]
  (sql/with-connection conn
    (sql/with-query-results
      res [query]
      (doall res))))

(defn -do-command [conn command]
  (sql/with-connection conn
    (sql-internal/do-prepared-return-keys* command nil)))

(deftype JdbcQueryExecutor [conn]
  QueryExecutor
  (do-query [this query] (-do-query conn query))
  (do-command [this command] (-do-command conn command)))

(defn new-jdbc-query-executor [conn]
  (JdbcQueryExecutor. conn))
