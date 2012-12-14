(ns hyperion.sql.transaction-spec
  (:require [speclj.core :refer :all]
    [hyperion.sql.query :refer [make-query]]
    [hyperion.sql.connection :refer [connection with-connection]]
    [hyperion.sql.jdbc :refer :all]))

(defn create-table [name]
  (execute-mutation
    (make-query (format "CREATE TABLE %s (name VARCHAR(20), age INTEGER)" name))))

(defn drop-table [name]
  (execute-mutation
    (make-query (format "DROP TABLE IF EXISTS %s" name))))

(defn test-count []
  (count (execute-query
    (make-query "SELECT * FROM test"))))

(defn do-query [query]
  (execute-write
    (make-query query)))

(defn include-transaction-specs [url]
  (describe "Transactions"
    (before
      (with-connection url
        (create-table "test")))

    (around [spec]
      (with-connection url
        (spec)))

    (after
      (with-connection url
        (drop-table "test")))

    (context "rollback"

      (it "rolls back all changes"
        (rollback
          (do-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")
          (should= 1 (test-count)))
        (should= 0 (test-count)))

      (it "resets auto commit to its previous value"
        (.setAutoCommit (connection) true)
        (try
          (rollback
            (throw (Exception.)))
          (catch Exception _))
        (should (.getAutoCommit (connection))))

      (it "rolls back multiple"
        (rollback
          (do-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")
          (rollback
            (do-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")
            (should= 2 (test-count)))
          (should= 1 (test-count)))
        (should= 0 (test-count)))

      (it "returns the result of the body"
        (should= :return (rollback :return)))

             )

    (context "transaction"
      (it "commits"
        (transaction
          (do-query "INSERT INTO test (name, age) VALUES ('Myles', 23)"))
          (should= 1 (test-count)))

      (it "rolls back when a runtime exception is thrown"
        (should-throw
          RuntimeException
          (try
            (transaction
              (do-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")
              (should= 1 (test-count))
              (throw (RuntimeException.)))
            (finally
              (should= 0 (test-count))))))

      (it "rolls back when a normal exception is thrown"
        (should-throw
          Exception
          (try
            (transaction
              (do-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")
              (should= 1 (test-count))
              (throw (IllegalStateException.)))
            (finally
              (should= 0 (test-count))))))

      (it "resets auto commit to its previous value"
        (.setAutoCommit (connection) true)
        (try
          (transaction
            (throw (IllegalStateException.)))
          (catch IllegalStateException _))
        (should (.getAutoCommit (connection))))


      (it "commits nested transactions"
        (transaction
          (do-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")
          (transaction
            (do-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")))
          (should= 2 (test-count)))

      (it "returns the result of the body"
        (should= {:thing "1"} (transaction {:thing "1"})))

      (it "rolls back nested transactions"
        (try
          (transaction
            (do-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")
            (try
              (transaction
                (do-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")
                (should= 2 (test-count))
                (throw (IllegalStateException.)))
              (catch IllegalStateException  _))
            (should= 1 (test-count))
            (throw (IllegalStateException.)))
          (catch IllegalStateException _))
          (should= 0 (test-count))))))
