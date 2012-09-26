(ns hyperion.postgres-spec
  (:use [speclj.core]
        [hyperion.sql.spec-helper]
        [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
        [hyperion.sql.transaction-spec :only [include-transaction-specs]]
        [hyperion.api :only [*ds* new-datastore save find-by-key]]
        [hyperion.sql.jdbc :only [execute-mutation]]
        [hyperion.sql.connection :only [with-connection-url]]
        [hyperion.sql.query]))

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
    account_key INTEGER REFERENCES account,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32)
    )"))

(defn create-table [table-name]
  (do-query (format create-table-query table-name)))

(defn drop-table [table-name]
  (do-query (format "DROP TABLE IF EXISTS %s" table-name)))

(describe "Postgres Datastore"
  (with connection-url "jdbc:postgresql://localhost:5432/hyperion")

  (context "creation"
    (it "with a kv pairs as params"
      (let [ds (new-datastore :implementation :postgres :connection-url @connection-url)]
        (should= false (.isClosed (.connection ds)))
        (.close (.connection ds)))))

  (context "live"

    (around [it]
      (binding [*ds* (new-datastore :implementation :postgres :connection-url @connection-url)]
        (it)))

    (before
      (with-connection-url @connection-url
        (create-table "testing")
        (create-table "other_testing")
        (create-key-tables)))

    (after
      (with-connection-url @connection-url
        (drop-table "testing")
        (drop-table "other_testing")
        (drop-table "shirt")
        (drop-table "account")))

    (it-behaves-like-a-datastore)

    (context "Transactions"
      (around [it]
        (with-connection-url @connection-url
          (it)))
      (include-transaction-specs))

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
    )
  )
