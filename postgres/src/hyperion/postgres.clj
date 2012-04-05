(ns hyperion.postgres
  (:use
    [hyperion.sql.datastore :only [new-sql-datastore]]
    [hyperion.postgres-qb]
    [hyperion.sql.jdbc-qe]))

(defn new-postgres-datastore [conn]
  (new-sql-datastore (new-jdbc-query-executor conn) (new-postgres-query-builder)))
