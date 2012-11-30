(ns hyperion.sqlite-spec
  (:use [speclj.core]
        [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
        [hyperion.api :only [*ds* new-datastore]]
        [hyperion.sql.connection :only [with-connection-url]]
        [hyperion.sql.jdbc :only [execute-mutation]]
        [hyperion.sql.query]
        [hyperion.sql.transaction-spec :only [include-transaction-specs]]
        [hyperion.sqlite :only [new-sqlite-datastore]]))

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

(describe "SQLite Datastore"
  (with connection-url "jdbc:sqlite:hyperion_clojure.sqlite")

  (context "creation"

    (it "with a kv pairs as params"
      (let [ds (new-sqlite-datastore :connection-url @connection-url)]
        (should= false (.isClosed (.connection ds)))
        (.close (.connection ds))))

    (it "with factory fn"
      (let [ds (new-datastore :implementation :sqlite :connection-url @connection-url)]
        (should= false (.isClosed (.connection ds)))
        (.close (.connection ds)))))

  (context "live"

    (around [it]
      (binding [*ds* (new-datastore :implementation :sqlite :connection-url @connection-url)]
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
        (drop-table "account")
        (drop-table "shirt")))

    (it-behaves-like-a-datastore)

    (context "Transactions"
      (around [it]
        (with-connection-url @connection-url
          (it)))
      (include-transaction-specs))))
