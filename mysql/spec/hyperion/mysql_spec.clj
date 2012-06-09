(ns hyperion.mysql-spec
  (:require
    [speclj.core :refer :all]
    [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]
    [hyperion.core :refer [*ds*]]
    [hyperion.mysql :refer [new-mysql-datastore]]
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
          [:birthdate :date]
          [:inti :int]
          [:data "VARCHAR(32)"]
          :table-spec "ENGINE=InnoDB" "")
        (sql/create-table
          :other_testing
          [:id :serial "PRIMARY KEY"]
          [:inti :int]
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
