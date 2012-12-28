(ns hyperion.sqlite-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [*ds* new-datastore defentity pack unpack]]
            [hyperion.log :as log]
            [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]
            [hyperion.sql.connection :refer [with-connection]]
            [hyperion.sql.jdbc :refer [execute-mutation execute-query]]
            [hyperion.sql.query :refer :all]
            [hyperion.sql.transaction-spec :refer [include-transaction-specs]]
            [hyperion.sqlite :refer [new-sqlite-datastore]]))

(log/error!)

(defn do-query [query]
  (execute-mutation
    (make-query query)))

(def create-table-query
  "CREATE TABLE IF NOT EXISTS %s (
    id INTEGER PRIMARY KEY,
    name VARCHAR(35),
    first_name VARCHAR(35),
    birthdate DATE,
    inti INTEGER,
    data VARCHAR(32)
  )")

(defn create-key-tables []
  (do-query
    "CREATE TABLE IF NOT EXISTS account (
    id INTEGER PRIMARY KEY,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32)
    );")
  (do-query
    "CREATE TABLE IF NOT EXISTS shirt (
    id INTEGER PRIMARY KEY,
    account_id INTEGER,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32),
    FOREIGN KEY (account_id) REFERENCES account(id)
    )"))

(defn create-types-table []
  (do-query
    "CREATE TABLE IF NOT EXISTS types (
    id INTEGER PRIMARY KEY,
    bool BOOLEAN,
    bite TINYINT,
    inti INTEGER,
    lng BIGINT,
    flt DOUBLE,
    dbl DOUBLE,
    data VARCHAR(32)
    )"))

(defentity :types
  [bool :type java.lang.Boolean]
  [bite :type java.lang.Byte]
  [inti]
  [flt :type java.lang.Float]
  [lng :type java.lang.Long]
  [dbl])

(defn create-table [table-name]
  (do-query (format create-table-query table-name)))

(defn drop-table [table-name]
  (do-query (format "DROP TABLE IF EXISTS %s" table-name)))

(defn table-exists? [table-name]
  (not (empty?
    (execute-query
    (make-query
      (format "SELECT name FROM sqlite_master WHERE type='table' AND name='%s'" table-name))))))

(defn wait-for-tables-to-exist [tables]
  (loop [[table & more] tables]
    (when table
      (if (table-exists? table)
        (recur more)
        (recur (cons table more))))))

(defn wait-for-tables-to-not-exist [tables]
  (loop [[table & more] tables]
    (when table
      (if (table-exists? table)
        (recur (cons table more))
        (recur more)))))

(def connection-url "jdbc:sqlite:hyperion_clojure.sqlite")
(def all-tables ["testing" "other_testing" "account" "shirt" "types"])

(describe "SQLite Datastore"
  (around [it]
    (binding [*ds* (new-datastore :implementation :sqlite
                                  :connection-url connection-url)]
      (it)))

  (before
    (with-connection connection-url
      (create-table "testing")
      (create-table "other_testing")
      (create-key-tables)
      (create-types-table)
      (wait-for-tables-to-exist all-tables)))

  (after
    (with-connection connection-url
      (doseq [table all-tables]
        (drop-table table))
      (wait-for-tables-to-not-exist all-tables)))

  (it-behaves-like-a-datastore)

  (context "Transactions"
    (include-transaction-specs connection-url)))
