(ns hyperion.sql.jdbc-spec
  (:use
    [speclj.core]
    [hyperion.sql.query :only [make-query]]
    [hyperion.sql.connection :only [with-connection-url connection]]
    [hyperion.sql.jdbc]))

(defn create-table [name]
  (execute-mutation
    (make-query (format "CREATE TABLE %s (name VARCHAR(20), age INTEGER)" name))))

(defn drop-table [name]
  (execute-mutation
    (make-query (format "DROP TABLE IF EXISTS %s" name))))

(clojure.lang.RT/loadClassForName "org.sqlite.JDBC")

(defn test-count []
  (count (execute-query
    (make-query "SELECT * FROM test"))))

(describe "JDBC Adapter"
  (with connection-url "jdbc:sqlite:")
  (around [it]
    (with-connection-url @connection-url
      (try
        (create-table "test")
        (it)
        (finally
          (drop-table "test")))))

  (context "rollback"

    (it "rolls back all changes"
      (rollback
        (execute-write
          (make-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")))
        (should= 0 (test-count)))

    (it "resets auto commit to its previous value"
      (.setAutoCommit (connection) true)
      (try
        (rollback
          (throw (Exception.)))
        (catch Exception _))
      (should (.getAutoCommit (connection)))))

  (context "transaction"
    (it "commits"
      (transaction
        (execute-write
          (make-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")))
        (should= 1 (test-count)))

    (it "rolls back when a runtime exception is thrown"
      (should-throw
        RuntimeException
        (try
          (transaction
            (execute-write
              (make-query "INSERT INTO test (name, age) VALUES ('Myles', 23)"))
            (should= 1 (test-count))
            (throw (RuntimeException.)))
          (finally
            (should= 0 (test-count))))))

    (it "rolls back when a normal exception is thrown"
      (should-throw
        Exception
        (try
          (transaction
            (execute-write
              (make-query "INSERT INTO test (name, age) VALUES ('Myles', 23)"))
            (should= 1 (test-count))
            (throw (Exception.)))
          (finally
            (should= 0 (test-count))))))

    (it "resets auto commit to its previous value"
      (.setAutoCommit (connection) true)
      (try
        (transaction
          (throw (Exception.)))
        (catch Exception _))
      (should (.getAutoCommit (connection))))


    (it "commits nested transactions"
      (transaction
        (execute-write
          (make-query "INSERT INTO test (name, age) VALUES ('Myles', 23)"))
        (transaction
          (execute-write
            (make-query "INSERT INTO test (name, age) VALUES ('Myles', 23)"))))
        (should= 2 (test-count)))

    (it "returns the result of the body"
      (should= {:thing "1"} (transaction {:thing "1"})))

    (it "rolls back nested transactions"
      (try
        (transaction
          (execute-write
            (make-query "INSERT INTO test (name, age) VALUES ('Myles', 23)"))
          (transaction
            (execute-write
              (make-query "INSERT INTO test (name, age) VALUES ('Myles', 23)")))
          (should= 2 (test-count))
          (throw (Exception.)))
        (catch Exception _))
        (should= 0 (test-count)))))
