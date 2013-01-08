(ns hyperion.postgres-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer :all]
            [hyperion.log :as log]
            [hyperion.sql.spec-helper :refer :all]
            [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]
            [hyperion.sql.transaction-spec :refer [include-transaction-specs]]
            [hyperion.sql.jdbc :refer [execute-mutation]]
            [hyperion.sql.connection :refer [with-connection]]
            [hyperion.sql.query :refer :all]))

(log/error!)

(defn do-query [query]
  (execute-mutation
    (make-query query)))

(defn create-testing-table [table-name]
  (do-query
    (format
      "CREATE TABLE IF NOT EXISTS %s (
        id SERIAL PRIMARY KEY,
        name VARCHAR(35),
        first_name VARCHAR(35),
        inti INTEGER,
        data VARCHAR(32)
      )"
      table-name)))

(defn create-key-tables []
  (do-query
    "CREATE TABLE IF NOT EXISTS account (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32)
    );
    CREATE TABLE IF NOT EXISTS shirt (
    id SERIAL PRIMARY KEY,
    account_id INTEGER REFERENCES account,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32)
    )"))

(defn create-types-table []
  (do-query
    "CREATE TABLE IF NOT EXISTS types (
    id SERIAL PRIMARY KEY,
    bool BOOLEAN,
    bite SMALLINT,
    shrt INTEGER,
    inti INTEGER,
    lng BIGINT,
    big_int NUMERIC,
    flt FLOAT,
    dbl FLOAT,
    str VARCHAR(35),
    kwd VARCHAR(35),
    data VARCHAR(32),
    first_name VARCHAR(35)
    )"))

(defn empty-tables [tables]
  (doseq [table tables]
    (do-query (format "TRUNCATE %s CASCADE" table))))

(defentity :types
  [bool]
  [bite :type java.lang.Byte]
  [shrt :type java.lang.Short]
  [inti]
  [lng]
  [flt :type java.lang.Float]
  [dbl]
  [str]
  [kwd :type clojure.lang.Keyword])

(def connection-url "jdbc:postgresql://localhost:5432/hyperion")
(def all-tables ["testing" "other_testing" "account" "shirt" "types"])

(describe "Postgres Datastore"
  (before-all
    (with-connection connection-url
      (create-testing-table "testing")
      (create-testing-table "other_testing")
      (create-key-tables)
      (create-types-table)))

  (around [it]
    (binding [*ds* (new-datastore :implementation :postgres :connection-url connection-url)]
      (it)))

  (before
    (with-connection connection-url
      (empty-tables all-tables)))

  (it-behaves-like-a-datastore)

  (context "SQL Injection"
    (it "sanitizes strings to be inserted"
      (let [evil-string "my evil string' --"
            record (save {:kind :testing :name evil-string})]
        (should= evil-string (:name (find-by-key (:key record))))))

    (it "sanitizes table names"
      (error-msg-contains?
        "relation \"my_evil_name\"___\" does not exist"
        (save {:kind "my-evil-name\" --" :name "test"})))

    (it "sanitizes column names"
      (error-msg-contains?
        "column \"my_evil_name\"___\" of relation \"testing\" does not exist"
        (save {:kind :testing (keyword "my-evil-name\" --") "test"}))))

  (context "Transactions"
    (include-transaction-specs connection-url))

  )
