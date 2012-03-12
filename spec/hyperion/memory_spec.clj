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

  (it "assigns key to new records"
    (let [saved (save {:kind "widget"})]
      (should= 2 (count saved))
      (should= "widget" (:kind saved))
      (should-not= nil (:key saved))))

  (it "assigned keys are unique"
    (should= 10 (count (set (map #(:key (save {:kind %})) (range 10))))))

  (it "finds the items by key"
    (let [one (save {:kind "one"})
          two (save {:kind "two"})]
      (should= one (find-by-key (:key one)))
      (should= two (find-by-key (:key two)))))

  (it "deletes records"
    (let [one (save {:kind "one"})]
      (delete one)
      (should= nil (find-by-key (:key one)))))

  (it "finds records by kind"
    (let [one (save {:kind "one"})
          two (save {:kind "two"})
          tre (save {:kind "tre"})]
      (should= [] (find-by-kind "foo"))
      (should= [one] (find-by-kind "one"))
      (should= [two] (find-by-kind "two"))
      (should= [tre] (find-by-kind "tre"))))

  (it "can save many records"
    (let [inf-records (map #(hash-map :kind "inf" :value %) (iterate inc 0))
          saved (save* (take 10 inf-records))]
      (should= 10 (count (set (map :key saved))))
      (should= 10 (count (find-by-kind "inf")))
      (should= (range 10) (sort (map :value (find-by-kind "inf"))))))

  (it "can delete many records"
    (let [inf-records (map #(hash-map :kind "inf" :value %) (iterate inc 0))
          saved (save* (take 10 inf-records))]
      (apply delete saved)
      (should= 0 (count (find-by-kind "inf")))))

  (context "searching"
    (with test-data [{:kind "thing" :int 1 :str "one"}
                     {:kind "thing" :int 12 :str "twelve"}
                     {:kind "thing" :int 23 :str "twenty3"}
                     {:kind "thing" :int 34 :str "thirty4"}
                     {:kind "thing" :int 45 :str "forty5"}
                     {:kind "who" :int 56 :str "fifty6"}])
    (before (save* @test-data))

    (it "applies filters to find-by-kind with ints"
      (should= #{1 12 23} (set (map :int (find-by-kind "thing" :filters [:< :int 25]))))
      (should= #{1 12 23} (set (map :int (find-by-kind "thing" :filters [:<= :int 23]))))
      (should= #{34 45} (set (map :int (find-by-kind "thing" :filters [:> :int 25]))))
      (should= #{34 45} (set (map :int (find-by-kind "thing" :filters [:>= :int 34]))))
      (should= #{34} (set (map :int (find-by-kind "thing" :filters [:= :int 34]))))
      (should= #{1 12 23 45} (set (map :int (find-by-kind "thing" :filters [:!= :int 34]))))
      (should= #{12 34} (set (map :int (find-by-kind "thing" :filters [:in :int [12 34]]))))
      (should= #{} (set (map :int (find-by-kind "thing" :filters [[:< :int 24] [:> :int 25]]))))
      (should= #{1 45} (set (map :int (find-by-kind "thing" :filters [[:!= :int 12] [:!= :int 23] [:!= :int 34]]))))
      )

    (it "applies filters to find-by-kind with strings"
      (should= #{"one" "forty5"} (set (map :str (find-by-kind "thing" :filters [:< :str "qux"]))))
      (should= #{"one" "forty5"} (set (map :str (find-by-kind "thing" :filters [:<= :str "one"]))))
      (should= #{"twelve" "twenty3" "thirty4"} (set (map :str (find-by-kind "thing" :filters [:> :str "qux"]))))
      (should= #{"twelve" "twenty3" "thirty4"} (set (map :str (find-by-kind "thing" :filters [:>= :str "thirty4"]))))
      (should= #{"one"} (set (map :str (find-by-kind "thing" :filters [:= :str "one"]))))
      (should= #{"twelve" "twenty3" "thirty4" "forty5"} (set (map :str (find-by-kind "thing" :filters [:!= :str "one"]))))
      (should= #{"one" "twelve"} (set (map :str (find-by-kind "thing" :filters [:in :str ["one" "twelve"]]))))
      (should= #{} (set (map :str (find-by-kind "thing" :filters [[:> :str "qux"] [:< :str "qux"]]))))
      (should= #{"thirty4" "forty5"} (set (map :str (find-by-kind "thing" :filters [[:!= :str "one"] [:!= :str "twelve"] [:!= :str "twenty3"]])))))

    (it "applies sorts to find-by-kind"
      (should= [1 12 23 34 45] (map :int (find-by-kind "thing" :sorts [:int :asc])))
      (should= [45 34 23 12 1] (map :int (find-by-kind "thing" :sorts [:int :desc])))
      (should= [45 1 34 12 23] (map :int (find-by-kind "thing" :sorts [:str :asc])))
      (should= [23 12 34 1 45] (map :int (find-by-kind "thing" :sorts [:str :desc])))
      (save {:kind "thing" :int 1 :str "the one"})
      (save {:kind "thing" :int 44 :str "forty5"})
      (should= ["one" "the one" "twelve" "twenty3" "thirty4" "forty5" "forty5"] (map :str (find-by-kind "thing" :sorts [[:int :asc] [:str :asc]])))
      (should= [44 45 1 1 34 12 23] (map :int (find-by-kind "thing" :sorts [[:str :asc] [:int :asc]]))))

    (it "sorts treat nil as last"
      (let [no-int (save {:kind "thing" :int nil :str "mo"})
            no-str (save {:kind "thing" :int 25 :str nil})]
        (should= no-int (last (find-by-kind "thing" :sorts [:int :asc])))
        (should= no-int (first (find-by-kind "thing" :sorts [:int :desc])))
        (should= no-str (last (find-by-kind "thing" :sorts [:str :asc])))
        (should= no-str (first (find-by-kind "thing" :sorts [:str :desc])))))

    (it "applies limit and offset to find-by-kind"
      (should= [1 12] (map :int (find-by-kind "thing" :sorts [:int :asc] :limit 2)))
      (should= [23 34] (map :int (find-by-kind "thing" :sorts [:int :asc] :limit 2 :offset 2)))
      (should= [45] (map :int (find-by-kind "thing" :sorts [:int :asc] :limit 2 :offset 4))))

    (it "counts by kind"
      (should= 5 (count-by-kind "thing"))
      (should= 3 (count-by-kind "thing" :filters [:> :int 20]))
      (should= 1 (count-by-kind "who")))

    (it "finds by all kinds (find-all-kinds)"
      (should= [1 12 23 34 45 56] (sort (map :int (find-all-kinds))))
      (should= [34 45 56] (map :int (find-all-kinds :filters [:> :int 30] :sorts [:int :asc]))))

    (it "counts by all kinds (count-all-kinds)"
      (should= 6 (count-all-kinds))
      (should= 3 (count-all-kinds :filters [:> :int 30] :sorts [:int :asc])))

    )
  )

(run-specs :stacktrace true)