(ns hyperion.postgres
  (:use
    [hyperion.sql.datastore :only [new-sql-datastore]]
    [hyperion.postgres-qb :only [new-postgres-query-builder]]
    [hyperion.sql.jdbc-qe]))

(defn new-postgres-datastore [conn database]
  (new-sql-datastore (new-jdbc-query-executor conn) (new-postgres-query-builder database)))
