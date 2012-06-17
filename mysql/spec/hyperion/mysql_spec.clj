(ns hyperion.mysql-spec
  (:use
    [speclj.core]
    [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
    [hyperion.core :only [*ds*]]
    [hyperion.mysql :only [new-mysql-datastore]])
  (:require
    [clojure.java.jdbc :as sql]))

(describe "MySQL Datastore"
  (with connection {:subprotocol "mysql"
                    :subname "//localhost:3306/hyperion"
                    :user "root"})
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
          [:data "VARCHAR(32)"]
          :table-spec "ENGINE=InnoDB" "")
        (sql/create-table
          :other_testing
          [:id :serial "PRIMARY KEY"]
          [:inti :int]
          [:name "VARCHAR(32)"]
          [:first_name "VARCHAR(32)"]
          [:data "VARCHAR(32)"]
          :table-spec "ENGINE=InnoDB" "")
        (binding [*ds* (new-mysql-datastore "hyperion")]
          (it))
        (catch Exception e
          (should-fail (str e)))
        (finally
          (sql/drop-table :testing)
          (sql/drop-table :other_testing)))))

  (it-behaves-like-a-datastore))
