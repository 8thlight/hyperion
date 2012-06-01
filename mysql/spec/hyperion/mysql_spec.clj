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
  (around [it]
    (sql/with-connection @connection
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
        :table-spec "ENGINE=InnoDB" ""))
    (reset! DS (new-mysql-datastore @connection "hyperion"))
    (try
      (it)
      (catch Exception e)
      (finally
        (sql/with-connection @connection
          (sql/drop-table :testing)
          (sql/drop-table :other_testing)))))

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
      (should= 10 (count (set (map #(:key (save {:kind "testing" :name %})) (range 10))))))

    (it "saves date types"
      (let [date-without-timestamp (java.util.Date. 50, 3, 20)
            record (save {:kind :testing :birthdate date-without-timestamp})]
        (should= date-without-timestamp (:birthdate record))
        (should= date-without-timestamp (:birthdate (find-by-key (:key record)))))))

  (context "save*"
    (it "can save many records"
      (let [inf-records (map #(hash-map :kind "testing" :name (str %)) (iterate inc 0))
            saved (save* (take 10 inf-records))]
        (should= 10 (count (set (map :key saved))))
        (should= 10 (count (find-by-kind "testing")))
        (should= (map str (range 10)) (sort (map :name (find-by-kind "testing")))))))

  (context "delete"
    (it "deletes records"
      (let [one (save {:kind "testing" :name "jim"})]
        (delete one)
        (should= nil (find-by-key (:key one))))))

  (context "searching"
    (with test-data (save* [{:kind "testing" :inti 1  :data "one"}
                            {:kind "testing" :inti 12 :data "twelve"}
                            {:kind "testing" :inti 23 :data "twenty3"}
                            {:kind "testing" :inti 34 :data "thirty4"}
                            {:kind "testing" :inti 45 :data "forty5"}
                            {:kind "testing" :inti 1 :data "the one"}
                            {:kind "testing" :inti 44 :data "forty4"}]))
    (with other-test-data (save {:kind "other_testing" :inti 56 :data "fifty6"}))
    (before @test-data @other-test-data)

    (it "applies filters to find-by-kind with ints"
      (should= #{1 12 23} (set (map :inti (find-by-kind "testing" :filters [:< :inti 25]))))
      (should= #{1 12 23} (set (map :inti (find-by-kind "testing" :filters [:<= :inti 23]))))
      (should= #{34 44 45} (set (map :inti (find-by-kind "testing" :filters [:> :inti 25]))))
      (should= #{34 44 45} (set (map :inti (find-by-kind "testing" :filters [:>= :inti 34]))))
      (should= #{34} (set (map :inti (find-by-kind "testing" :filters [:= :inti 34]))))
      (should= #{1 12 23 44 45} (set (map :inti (find-by-kind "testing" :filters [:!= :inti 34]))))
      (should= #{12 34} (set (map :inti (find-by-kind "testing" :filters [:in :inti [12 34]]))))
      (should= #{} (set (map :inti (find-by-kind "testing" :filters [[:< :inti 24] [:> :inti 25]]))))
      (should= #{1 44 45} (set (map :inti (find-by-kind "testing" :filters [[:!= :inti 12] [:!= :inti 23] [:!= :inti 34]])))))

      (it "applies filters to find-by-kind with strings"
        (should= #{"one" "forty4" "forty5"} (set (map :data (find-by-kind "testing" :filters [:< :data "qux"]))))
        (should= #{"one" "forty4" "forty5"} (set (map :data (find-by-kind "testing" :filters [:<= :data "one"]))))
        (should= #{"the one" "twelve" "twenty3" "thirty4"} (set (map :data (find-by-kind "testing" :filters [:> :data "qux"]))))
        (should= #{"twelve" "twenty3" "thirty4"} (set (map :data (find-by-kind "testing" :filters [:>= :data "thirty4"]))))
        (should= #{"one"} (set (map :data (find-by-kind "testing" :filters [:= :data "one"]))))
        (should= #{"the one" "twelve" "twenty3" "thirty4" "forty4" "forty5"} (set (map :data (find-by-kind "testing" :filters [:!= :data "one"]))))
        (should= #{"one" "twelve"} (set (map :data (find-by-kind "testing" :filters [:in :data ["one" "twelve"]]))))
        (should= #{} (set (map :data (find-by-kind "testing" :filters [[:> :data "qux"] [:< :data "qux"]]))))
        (should= #{"the one" "thirty4"  "forty4" "forty5"} (set (map :data (find-by-kind "testing" :filters [[:!= :data "one"] [:!= :data "twelve"] [:!= :data "twenty3"]])))))

      (it "applies sorts to find-by-kind"
        (should= [1 1 12 23 34 44 45] (map :inti (find-by-kind "testing" :sorts [:inti :asc])))
        (should= [45 44 34 23 12 1 1] (map :inti (find-by-kind "testing" :sorts [:inti :desc])))
        (should= [44 45 1 1 34 12 23] (map :inti (find-by-kind "testing" :sorts [:data :asc])))
        (should= [23 12 34 1 1 45 44] (map :inti (find-by-kind "testing" :sorts [:data :desc])))
        (should= ["one" "the one" "twelve" "twenty3" "thirty4" "forty4" "forty5"] (map :data (find-by-kind "testing" :sorts [[:inti :asc] [:data :asc]])))
        (should= [44 45 1 1 34 12 23] (map :inti (find-by-kind "testing" :sorts [[:data :asc] [:inti :asc]]))))

      (it "sorts treat nil as last"
        (let [no-inti (save {:kind "testing" :inti nil :data "mo"})
              no-data (save {:kind "testing" :inti 25 :data nil})]
          (should= no-inti (last (find-by-kind "testing" :sorts [:inti :asc])))
          (should= no-inti (first (find-by-kind "testing" :sorts [:inti :desc])))
          (should= no-data (last (find-by-kind "testing" :sorts [:data :asc])))
          (should= no-data (first (find-by-kind "testing" :sorts [:data :desc])))))

      (it "applies limit and offset to find-by-kind"
        (should= [1 1] (map :inti (find-by-kind "testing" :sorts [:inti :asc] :limit 2)))
        (should= [12 23] (map :inti (find-by-kind "testing" :sorts [:inti :asc] :limit 2 :offset 2)))
        (should= [34 44] (map :inti (find-by-kind "testing" :sorts [:inti :asc] :limit 2 :offset 4))))

      (it "counts by kind"
        (should= 7 (count-by-kind "testing"))
        (should= 4 (count-by-kind "testing" :filters [:> :inti 20])))

      (it "finds by all kinds (find-all-kinds)"
        (should= [1 1 12 23 34 44 45 56] (sort (map :inti (find-all-kinds))))
        (should= [34 44 45 56] (map :inti (find-all-kinds :filters [:> :inti 30] :sorts [:inti :asc]))))

      (it "counts by all kinds (count-all-kinds)"
        (should= 8 (count-all-kinds))
        (should= 4 (count-all-kinds :filters [:> :inti 30] :sorts [:inti :asc])))

      (it "finds all records by kind"
        (should= (sort (map :key @test-data)) (sort (map :key (find-by-kind "testing")))))

      (it "finds the items by key"
        (let [one (save {:kind "testing" :data "my stuff"})]
          (should= one (find-by-key (:key one)))))))

