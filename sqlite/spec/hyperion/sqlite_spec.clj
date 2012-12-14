(ns hyperion.sqlite-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [*ds* new-datastore]]
            [hyperion.log :as log]
            [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]
            [hyperion.sql.connection :refer [with-connection]]
            [hyperion.sql.jdbc :refer [execute-mutation]]
            [hyperion.sql.query :refer :all]
            [hyperion.sql.transaction-spec :refer [include-transaction-specs]]
            [hyperion.sqlite :refer [new-sqlite-datastore]]))

(log/error!)

(defn do-query [query]
  (execute-mutation
    (make-query query)))

(def create-table-query
  "CREATE TABLE %s (
    id INTEGER PRIMARY KEY,
    name VARCHAR(35),
    first_name VARCHAR(35),
    birthdate DATE,
    inti INTEGER,
    data VARCHAR(32)
  )")

(defn create-key-tables []
  (do-query
    "CREATE TABLE account (
    id INTEGER PRIMARY KEY,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32)
    );")
  (do-query
    "CREATE TABLE shirt (
    id INTEGER PRIMARY KEY,
    account_id INTEGER,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32),
    FOREIGN KEY (account_id) REFERENCES account(id)
    )"))

(defn create-table [table-name]
  (do-query (format create-table-query table-name)))

(defn drop-table [table-name]
  (do-query (format "DROP TABLE IF EXISTS %s" table-name)))

(def connection-url "jdbc:sqlite:hyperion_clojure.sqlite")

(describe "SQLite Datastore"
  (around [it]
    (binding [*ds* (new-datastore :implementation :sqlite :connection-url connection-url)]
      (it)))

  (before
    (with-connection connection-url
      (create-table "testing")
      (create-table "other_testing")
      (create-key-tables)))

  (after
    (with-connection connection-url
      (drop-table "testing")
      (drop-table "other_testing")
      (drop-table "account")
      (drop-table "shirt")))

  (it-behaves-like-a-datastore)

  (context "Transactions"
    (include-transaction-specs connection-url)))
