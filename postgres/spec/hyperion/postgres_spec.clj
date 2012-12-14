(ns hyperion.postgres-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [*ds* new-datastore save find-by-key]]
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

(def create-table-query
  "CREATE TABLE %s (
    id SERIAL PRIMARY KEY,
    name VARCHAR(35),
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32)
  )")

(defn create-key-tables []
  (do-query
    "CREATE TABLE account (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32)
    );
    CREATE TABLE shirt (
    id SERIAL PRIMARY KEY,
    account_id INTEGER REFERENCES account,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32)
    )"))

(defn create-table [table-name]
  (do-query (format create-table-query table-name)))

(defn drop-table [table-name]
  (do-query (format "DROP TABLE IF EXISTS %s" table-name)))

(def connection-url "jdbc:postgresql://localhost:5432/hyperion")

(describe "Postgres Datastore"
  (around [it]
    (binding [*ds* (new-datastore :implementation :postgres :connection-url connection-url)]
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
      (drop-table "shirt")
      (drop-table "account")))

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
