(ns hyperion.postgres-spec
  (:use [speclj.core]
        [hyperion.sql.spec-helper]
        [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
        [hyperion.api :only [*ds*]]
        [hyperion.sql.jdbc :only [execute-mutation]]
        [hyperion.sql.connection :only [connection]]
        [hyperion.sql.query]
        [hyperion.postgres :only [new-postgres-datastore]]))

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

(describe "Postgres Datastore"

  (context "creation"

    (it "with a string as the only param"
      (let [ds (new-postgres-datastore "jdbc:postgresql://localhost:5432/hyperion")]
        (should= false (.isClosed (.connection ds)))
        (.close (.connection ds))))

    (it "with a kv pairs as params"
      (let [ds (new-postgres-datastore :connection-url "jdbc:postgresql://localhost:5432/hyperion")]
        (should= false (.isClosed (.connection ds)))
        (.close (.connection ds))))

    )

  (context "live"

    (with-connection-and-rollback "jdbc:postgresql://localhost:5432/hyperion")

    (around [it]
      (create-table "testing")
      (create-table "other_testing")
      (binding [*ds* (new-postgres-datastore :connection (connection))]
        (it)))

    (it-behaves-like-a-datastore)
    )
  )
