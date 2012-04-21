(ns hyperion.mysql-spec
  (:require
    [speclj.core :refer :all]
    [hyperion.core :refer :all]
    [hyperion.mysql :refer [new-mysql-datastore]]
    [clojure.java.jdbc :as sql]))

(describe "MySQL Datastore"
  (with connection {:subprotocol "mysql"
                    :subname "//localhost:3306/hyperion"
                    :user "root"})
  (before
    (sql/with-connection @connection
      (sql/create-table
        :testing [:id :serial "PRIMARY KEY"] [:name "VARCHAR(32)"] [:birthdate :date] :table-spec "ENGINE=InnoDB" ""))
    (reset! DS (new-mysql-datastore @connection "hyperion")))
  (after
    (sql/with-connection @connection
      (sql/drop-table :testing)))

  (context "save"
    (it "saves a map with kind as a string and returns it"
      (let [record (save {:kind "testing" :name "ann"})]
        (should= "testing" (:kind record))
        (should= "ann" (:name record))))

    (it "saves a map with kind as a symbol and returns it"
      (let [record (save {:kind :testing :name "ann"})]
        (should= "testing" (:kind record))
        (should= "ann" (:name record))))

    (it "it saves an existing record"
      (let [record1 (save {:kind "testing" :name "ann"})
            record2 (save (assoc record1 :name "james"))]
        (should= (:key record1) (:key record2))
        (should= 1 (count (find-by-kind "testing")))))

    (it "assigns key to new records"
      (let [record (save {:kind "testing" :name "ann"})]
        (should= "testing-1" (:key record))))

    (it "assigned keys are unique"
      (should= 10 (count (set (map #(:key (save {:kind "testing" :name %})) (range 10))))))))
