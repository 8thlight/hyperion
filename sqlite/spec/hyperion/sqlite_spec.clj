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

(defn create-testing-table [table]
  (do-query
    (format
      "CREATE TABLE IF NOT EXISTS %s (
      id INTEGER PRIMARY KEY,
      name VARCHAR(35),
      first_name VARCHAR(35),
      birthdate DATE,
      inti INTEGER,
      data VARCHAR(32)
      )"
      table)))

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
    shrt INTEGER,
    inti INTEGER,
    lng INTEGER,
    big_int BLOB,
    flt DOUBLE,
    dbl DOUBLE,
    str VARCHAR(32),
    kwd VARCHAR(32),
    data VARCHAR(32)
    )"))

(defn empty-tables [tables]
  (doseq [table tables]
    (do-query (format "DELETE FROM %s" table))))

(defentity :types
  [bool :type java.lang.Boolean]
  [bite :type java.lang.Byte]
  [shrt :type java.lang.Short]
  [inti]
  [lng :type java.lang.Long]
  [flt :type java.lang.Float]
  [dbl]
  [str]
  [kwd :type clojure.lang.Keyword])

(def all-tables ["testing" "other_testing" "account" "shirt" "types"])
(def connection-url "jdbc:sqlite:hyperion_clojure.sqlite")

(describe "SQLite Datastore"
  (before-all
    (with-connection connection-url
      (create-testing-table "testing")
      (create-testing-table "other_testing")
      (create-key-tables)
      (create-types-table)))

  (around [it]
    (with-connection connection-url
      (binding [*ds* (new-datastore :implementation :sqlite
                                    :connection-url connection-url)]
        (it))))

  (before
    (empty-tables all-tables))

  (it-behaves-like-a-datastore)

  (context "Transactions"
    (include-transaction-specs connection-url)))
