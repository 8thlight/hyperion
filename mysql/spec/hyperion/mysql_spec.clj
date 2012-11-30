(ns hyperion.mysql-spec
  (:use [speclj.core]
        [hyperion.sql.spec-helper]
        [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
        [hyperion.sql.transaction-spec :only [include-transaction-specs]]
        [hyperion.api :only [*ds* new-datastore save find-by-key]]
        [hyperion.sql.connection :only [with-connection-url]]
        [hyperion.sql.jdbc :only [execute-mutation]]
        [hyperion.sql.query]))

(defn do-query [query]
  (execute-mutation
    (make-query query)))

(def create-table-query
  "CREATE TABLE %s (
    id INTEGER NOT NULL AUTO_INCREMENT,
    name VARCHAR(35),
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32),
    PRIMARY KEY (id)
  )")

(defn create-key-tables []
  (do-query
    "CREATE TABLE account (
    id INTEGER NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32),
    PRIMARY KEY (id)
    );")
  (do-query
    "CREATE TABLE shirt (
    id INTEGER NOT NULL AUTO_INCREMENT,
    account_key INTEGER,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32),
    PRIMARY KEY (id),
    INDEX (account_key),
    FOREIGN KEY (account_key) REFERENCES account (id)
    )"))

(defn create-table [table-name]
  (do-query (format create-table-query table-name)))

(defn drop-table [table-name]
  (do-query (format "DROP TABLE IF EXISTS %s" table-name)))

(describe "MySQL Datastore"
  (with connection-url "jdbc:mysql://localhost:3306/hyperion?user=root")

  (it "with factory fn"
    (let [ds (new-datastore :implementation :mysql :connection-url @connection-url :database "hyperion")]
      (should= "hyperion" (.database (.db ds)))))

  (context "live"

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

    (around [it]
      (binding [*ds* (new-datastore :implementation :mysql :connection-url @connection-url :database "hyperion")]
        (it)))

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
          "Table 'hyperion.my_evil_name`___' doesn't exist"
          (save {:kind "my-evil-name` --" :name "test"})))

      (it "sanitizes column names"
        (error-msg-contains?
          "Unknown column 'my_evil_name`___' in 'field list'"
          (save {:kind :testing (keyword "my-evil-name` --") "test"}))))
    )
  )
