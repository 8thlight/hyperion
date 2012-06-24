(ns hyperion.mysql-spec
  (:use
    [speclj.core]
    [hyperion.sql.spec-helper]
    [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
    [hyperion.core :only [*ds*]]
    [hyperion.sql.jdbc :only [execute-mutation]]
    [hyperion.sql.query]
    [hyperion.mysql :only [new-mysql-datastore]]))

(def create-table-query
  "CREATE TABLE %s (
    id INTEGER NOT NULL AUTO_INCREMENT,
    name VARCHAR(35),
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32),
    PRIMARY KEY (id)
  )")

(defn create-table [table-name]
  (execute-mutation
    (make-query (format create-table-query table-name))))

(def drop-table-query "DROP TABLE IF EXISTS %s")

(defn drop-table [table-name]
  (execute-mutation
    (make-query (format drop-table-query table-name))))

(describe "MySQL Datastore"
  (with-connection-and-rollback "jdbc:mysql://localhost:3306/hyperion?user=root")

  (around [it]
    (try
      (create-table "testing")
      (create-table "other_testing")
      (binding [*ds* (new-mysql-datastore "hyperion")]
        (it))
      (finally
        (drop-table "testing")
        (drop-table "other_testing")
        )))

  (it-behaves-like-a-datastore))
