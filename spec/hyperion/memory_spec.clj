(ns hyperion.memory-spec
  (:use
    [speclj.core]
    [hyperion.core]
    [hyperion.memory]))

(describe "Memory Implementation"

  (with _ds (new-memory-datastore))
  (before (reset! DS @_ds))

  (it "can be created"
    (should= {} @(.store @_ds)))

  (it "can be created with starting stuff"
    (let [ds (new-memory-datastore {"some-key" {:kind "mine" :value 42}})]
      (should= {"some-key" {:kind "mine" :value 42}} @(.store ds))))

  (context "save"
    (it "saves a map with kind as a string and returns it"
      (let [record (save {:kind "testing" :data "hello"})]
        (should= "testing" (:kind record))
        (should= "hello" (:data record))))

    (it "saves a map with kind as a symbol and returns it"
      (let [record (save {:kind :testing :data "hello"})]
        (should= :testing (:kind record))
        (should= "hello" (:data record))))

    (it "assigns key to new records"
      (let [record (save {:kind "testing" :data "hello"})]
        (should-not= nil (:key record))))

    (it "assigned keys are unique"
      (should= 10 (count (set (map #(:key (save {:kind "testing" :data %})) (range 10)))))))

  (context "delete"
    (it "deletes records"
      (let [one (save {:kind "testing" :data "delete me"})]
        (delete one)
        (should= nil (find-by-key (:key one))))))

  (context "save*"
    (it "can save many records"
      (let [inf-records (map #(hash-map :kind "testing" :int %) (iterate inc 0))
            saved (save* (take 10 inf-records))]
        (should= 10 (count (set (map :key saved))))
        (should= 10 (count (find-by-kind "testing")))
        (should= (range 10) (sort (map :int (find-by-kind "testing")))))))

  (context "searching"
    (with test-data (save* [{:kind "testing" :int 1  :data "one"}
                            {:kind "testing" :int 12 :data "twelve"}
                            {:kind "testing" :int 23 :data "twenty3"}
                            {:kind "testing" :int 34 :data "thirty4"}
                            {:kind "testing" :int 45 :data "forty5"}
                            {:kind "testing" :int 1 :data "the one"}
                            {:kind "testing" :int 44 :data "forty4"}]))
    (with other-test-data (save {:kind "other_testing" :int 56 :data "fify6"}))
    (before @test-data @other-test-data)

    (it "applies filters to find-by-kind with ints"
      (should= #{1 12 23} (set (map :int (find-by-kind "testing" :filters [:< :int 25]))))
      (should= #{1 12 23} (set (map :int (find-by-kind "testing" :filters [:<= :int 23]))))
      (should= #{34 44 45} (set (map :int (find-by-kind "testing" :filters [:> :int 25]))))
      (should= #{34 44 45} (set (map :int (find-by-kind "testing" :filters [:>= :int 34]))))
      (should= #{34} (set (map :int (find-by-kind "testing" :filters [:= :int 34]))))
      (should= #{1 12 23 44 45} (set (map :int (find-by-kind "testing" :filters [:!= :int 34]))))
      (should= #{12 34} (set (map :int (find-by-kind "testing" :filters [:in :int [12 34]]))))
      (should= #{} (set (map :int (find-by-kind "testing" :filters [[:< :int 24] [:> :int 25]]))))
      (should= #{1 44 45} (set (map :int (find-by-kind "testing" :filters [[:!= :int 12] [:!= :int 23] [:!= :int 34]])))))

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
      (should= [1 1 12 23 34 44 45] (map :int (find-by-kind "testing" :sorts [:int :asc])))
      (should= [45 44 34 23 12 1 1] (map :int (find-by-kind "testing" :sorts [:int :desc])))
      (should= [44 45 1 1 34 12 23] (map :int (find-by-kind "testing" :sorts [:data :asc])))
      (should= [23 12 34 1 1 45 44] (map :int (find-by-kind "testing" :sorts [:data :desc])))
      (should= ["one" "the one" "twelve" "twenty3" "thirty4" "forty4" "forty5"] (map :data (find-by-kind "testing" :sorts [[:int :asc] [:data :asc]])))
      (should= [44 45 1 1 34 12 23] (map :int (find-by-kind "testing" :sorts [[:data :asc] [:int :asc]]))))

    (it "sorts treat nil as last"
      (let [no-int (save {:kind "testing" :int nil :data "mo"})
            no-data (save {:kind "testing" :int 25 :data nil})]
        (should= no-int (last (find-by-kind "testing" :sorts [:int :asc])))
        (should= no-int (first (find-by-kind "testing" :sorts [:int :desc])))
        (should= no-data (last (find-by-kind "testing" :sorts [:data :asc])))
        (should= no-data (first (find-by-kind "testing" :sorts [:data :desc])))))

    (it "applies limit and offset to find-by-kind"
      (should= [1 1] (map :int (find-by-kind "testing" :sorts [:int :asc] :limit 2)))
      (should= [12 23] (map :int (find-by-kind "testing" :sorts [:int :asc] :limit 2 :offset 2)))
      (should= [34 44] (map :int (find-by-kind "testing" :sorts [:int :asc] :limit 2 :offset 4))))

    (it "counts by kind"
      (should= 7 (count-by-kind "testing"))
      (should= 4 (count-by-kind "testing" :filters [:> :int 20]))
      (should= 1 (count-by-kind "other_testing")))

    (it "finds by all kinds (find-all-kinds)"
      (should= [1 1 12 23 34 44 45 56] (sort (map :int (find-all-kinds))))
      (should= [34 44 45 56] (map :int (find-all-kinds :filters [:> :int 30] :sorts [:int :asc]))))

    (it "counts by all kinds (count-all-kinds)"
      (should= 8 (count-all-kinds))
      (should= 4 (count-all-kinds :filters [:> :int 30] :sorts [:int :asc])))

    (it "finds all records by kind"
      (should= (sort (map :key @test-data)) (sort (map :key (find-by-kind "testing")))))

    (it "finds the items by key"
      (let [one (save {:kind "testing" :data "my stuff"})
            two (save {:kind "other_testing" :data "my other stuff"})]
        (should= one (find-by-key (:key one)))
        (should= two (find-by-key (:key two)))))))
