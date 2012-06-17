(ns hyperion.postgres-spec
  (:use
    [speclj.core]
    [hyperion.core :only [*ds*]]
    [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
    [hyperion.postgres :only [new-postgres-datastore]]
    )
  (:require
    [clojure.string :as str]
    [clojure.java.jdbc :as sql]))

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
          [:first_name "VARCHAR(32)"]
          [:birthdate :date]
          [:inti :int]
          [:data "VARCHAR(32)"])
        (sql/create-table
          :other_testing
          [:id :serial "PRIMARY KEY"]
          [:inti :int]
          [:name "VARCHAR(32)"]
          [:first_name "VARCHAR(32)"]
          [:data "VARCHAR(32)"])
        (binding [*ds* (new-postgres-datastore)]
          (it))
        (catch Exception e
          (should-fail (str e)))
        (finally
          (sql/drop-table :testing)
          (sql/drop-table :other_testing)))))

  (it-behaves-like-a-datastore))
