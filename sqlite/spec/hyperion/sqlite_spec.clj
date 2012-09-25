(ns hyperion.sqlite-spec
  (:use [speclj.core]
        [hyperion.sql.spec-helper]
        [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
        [hyperion.api :only [*ds* new-datastore]]
        [hyperion.sql.connection :only [connection]]
        [hyperion.sql.jdbc :only [execute-mutation]]
        [hyperion.sql.query]
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
    account_key INTEGER,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32),
    FOREIGN KEY (account_key) REFERENCES account(id)
    )"))

(defn create-table [table-name]
  (do-query (format create-table-query table-name)))

(describe "SQLite Datastore"

  (context "creation"

    (it "with a kv pairs as params"
      (let [ds (new-sqlite-datastore :connection-url "jdbc:sqlite:")]
        (should= false (.isClosed (.connection ds)))
        (.close (.connection ds))))

    (it "with factory fn"
      (let [ds (new-datastore :implementation :sqlite :connection-url "jdbc:sqlite:")]
        (should= false (.isClosed (.connection ds)))
        (.close (.connection ds))))

    )

  (context "live"

    (with-connection-and-rollback "jdbc:sqlite:")

    (around [it]
      (binding [*ds* (new-sqlite-datastore :connection (connection))]
        (it)))

    (before
      (create-table "testing")
      (create-table "other_testing")
      (create-key-tables))

    (it-behaves-like-a-datastore)
    )
  )
