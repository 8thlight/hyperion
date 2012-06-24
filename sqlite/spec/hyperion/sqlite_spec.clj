(ns hyperion.sqlite-spec
  (:use
    [speclj.core]
    [hyperion.sql.spec-helper]
    [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
    [hyperion.core :only [*ds*]]
    [hyperion.sql.connection :only [with-connection-url]]
    [hyperion.sql.jdbc :only [execute-mutation]]
    [hyperion.sql.query]
    [hyperion.sqlite :only [new-sqlite-datastore]]))

(def create-table-query
  "CREATE TABLE %s (
    id INTEGER PRIMARY KEY,
    name VARCHAR(35),
    first_name VARCHAR(35),
    birthdate DATE,
    inti INTEGER,
    data VARCHAR(32)
  )")

(defn create-table [table-name]
  (execute-mutation
    (make-query (format create-table-query table-name))))

(describe "SQLite Datastore"
  (around [it]
    (binding [*ds* (new-sqlite-datastore)]
      (it)))
  (with-connection-and-rollback "jdbc:sqlite:")

  (before
    (create-table "testing")
    (create-table "other_testing"))

  (it-behaves-like-a-datastore))
