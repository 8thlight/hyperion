(ns hyperion.postgres-spec
  (:use [speclj.core]
        [hyperion.sql.spec-helper]
        [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
        [hyperion.api :only [*ds* save find-by-key]]
        [hyperion.sql.jdbc :only [execute-mutation]]
        [hyperion.sql.connection :only [connection]]
        [hyperion.sql.query]
        [hyperion.postgres :only [new-postgres-datastore]])
  (:import [speclj SpecFailure]))

(def create-table-query
  "CREATE TABLE %s (
    id SERIAL PRIMARY KEY,
    name VARCHAR(35),
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32)
  )")

(defn create-table [table-name]
  (execute-mutation
    (make-query (format create-table-query table-name))))

(def connection-url "jdbc:postgresql://localhost:5432/hyperion")

(describe "Postgres Datastore"

  (context "creation"

    (it "with a string as the only param"
      (let [ds (new-postgres-datastore connection-url)]
        (should= false (.isClosed (.connection ds)))
        (.close (.connection ds))))

    (it "with a kv pairs as params"
      (let [ds (new-postgres-datastore :connection-url connection-url)]
        (should= false (.isClosed (.connection ds)))
        (.close (.connection ds))))

    )

  (context "live"

    (with-rollback connection-url)

    (around [it]
      (create-table "testing")
      (create-table "other_testing")
      (binding [*ds* (new-postgres-datastore :connection (connection))]
        (it)))

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
    )
  )
