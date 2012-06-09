(ns hyperion.postgres-spec
  (:require
    [clojure.string :as str]
    [clojure.java.jdbc :as sql]
    [speclj.core :refer :all]
    [hyperion.core :refer [*ds*]]
    [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]
    [hyperion.postgres :refer [new-postgres-datastore]]))

(describe "Postgres Datastore"
  (with connection {:subprotocol "postgresql"
                    :subname "hyperion"})

  (around [it]
    (sql/with-connection @connection
      (try
        (sql/create-table
          :testing
          [:id :serial "PRIMARY KEY"]
          [:name "VARCHAR(32)"]
          [:birthdate :date]
          [:inti :int]
          [:data "VARCHAR(32)"])
        (sql/create-table
          :other_testing
          [:id :serial "PRIMARY KEY"]
          [:inti :int]
          [:data "VARCHAR(32)"])
        (binding [*ds* (new-postgres-datastore)]
          (it))
        (catch Exception e
          (should-fail (str e)))
        (finally
          (sql/drop-table :testing)
          (sql/drop-table :other_testing)))))

  (it-behaves-like-a-datastore))
