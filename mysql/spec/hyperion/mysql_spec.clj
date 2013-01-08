(ns hyperion.mysql-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer :all]
            [hyperion.log :as log]
            [hyperion.sql.spec-helper :refer :all]
            [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]
            [hyperion.sql.transaction-spec :refer [include-transaction-specs]]
            [hyperion.sql.connection :refer [with-connection]]
            [hyperion.sql.jdbc :refer [execute-mutation]]
            [hyperion.sql.query :refer :all]
            [hyperion.mysql]))

(log/error!)

(defn do-query [query]
  (execute-mutation
    (make-query query)))

(defn create-testing-table [table]
  (do-query
    (format
      "CREATE TABLE IF NOT EXISTS %s (
      id INTEGER NOT NULL AUTO_INCREMENT,
      name VARCHAR(35),
      first_name VARCHAR(35),
      inti INTEGER,
      data VARCHAR(32),
      PRIMARY KEY (id)
      )"
      table)))

(defn create-key-tables []
  (do-query
    "CREATE TABLE IF NOT EXISTS account (
    id INTEGER NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32),
    PRIMARY KEY (id)
    );")
  (do-query
    "CREATE TABLE IF NOT EXISTS shirt (
    id INTEGER NOT NULL AUTO_INCREMENT,
    account_id INTEGER,
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32),
    PRIMARY KEY (id),
    INDEX (account_id),
    FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE
    )"))

(defn create-types-table []
  (do-query
    "CREATE TABLE IF NOT EXISTS types (
    id INTEGER NOT NULL AUTO_INCREMENT,
    bool BOOLEAN,
    bite TINYINT,
    shrt INTEGER,
    inti INTEGER,
    lng BIGINT,
    big_int BLOB,
    flt DOUBLE,
    dbl DOUBLE,
    str VARCHAR(32),
    chr VARCHAR(2),
    kwd VARCHAR(32),
    data VARCHAR(32),
    first_name VARCHAR(35),
    PRIMARY KEY (id)
    )"))

(defn empty-tables [tables]
  (doseq [table tables]
    (do-query (format "DELETE FROM %s" table))))

(defentity :types
  [bool]
  [bite :type java.lang.Byte]
  [shrt :type java.lang.Short]
  [inti]
  [lng]
  [flt :type java.lang.Float]
  [dbl]
  [str]
  [chr :type java.lang.Character]
  [kwd :type clojure.lang.Keyword])

(def connection-url "jdbc:mysql://localhost:3306/hyperion?user=root")
(def all-tables ["testing" "other_testing" "account" "shirt" "types"])

(describe "MySQL Datastore"

  (it "with factory fn"
    (let [ds (new-datastore :implementation :mysql :connection-url connection-url :database "hyperion")]
      (should= "hyperion" (.database (.db ds)))))

  (context "live"
    (before-all
      (with-connection connection-url
        (create-testing-table "testing")
        (create-testing-table "other_testing")
        (create-key-tables)
        (create-types-table)))

    (around [it]
      (binding [*ds* (new-datastore :implementation :mysql :connection-url connection-url :database "hyperion")]
        (it)))

    (before
      (with-connection connection-url
        (empty-tables all-tables)))

    (it-behaves-like-a-datastore)

    (context "Transactions"
      (include-transaction-specs connection-url))

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
