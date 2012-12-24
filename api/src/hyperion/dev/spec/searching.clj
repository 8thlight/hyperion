(ns hyperion.dev.spec.searching
  (:require [speclj.core :refer :all ]
            [hyperion.api :refer [save save* find-by-kind find-all-kinds count-by-kind find-by-key count-all-kinds]]
            [hyperion.key :refer [compose-key]]))

(defn it-can-find [find name]
  (list
    (it "returns the kind"
      (should (every? #(= "testing" (:kind %)) (find))))

    (defn intis [records]
      (map :inti records))

    (defn datas [records]
      (map :data records))

    (for [[expected filters]
          [
           [[1 1 12 23]           [:< :inti 25]]
           [[1 1 12 23]           [:<= :inti 23]]
           [[34 44 45]            [:> :inti 25]]
           [[34 44 45]            [:>= :inti 34]]
           [[34]                  [:= :inti 34]]
           [[nil]                 [:= :inti nil]]
           [[nil 1 1 12 23 44 45] [:!= :inti 34]]
           [[1 1 12 23 34 44 45]  [:!= :inti nil]]
           [[12 34]               [:in :inti [12 34]]]
           [[12 nil]              [:in :inti [12 nil]]]
           [[12 34]               [:contains? :inti [12 34]]]
           [[]                    [[:< :inti 24] [:> :inti 25]]]
           [[nil 1 1 44 45]       [[:!= :inti 12] [:!= :inti 23] [:!= :inti 34]]]
           [[nil 1 1 44 45]       [[:not :inti 12] [:not :inti 23] [:not :inti 34]]]
           ]]
      (it (str "filters ints to " expected " with filters: " filters)
        (should== expected (intis (find :filters filters)))))

    (for [[expected filters]
          [
           [["" "one" "forty4" "forty5"]                                  [:< :data "qux"]]
           [["" "one" "forty4" "forty5"]                                  [:<= :data "one"]]
           [["the one" "twelve" "twenty3" "thirty4"]                      [:> :data "qux"]]
           [["twelve" "twenty3" "thirty4"]                                [:>= :data "thirty4"]]
           [["one"]                                                       [:= :data "one"]]
           [["" "the one" "twelve" "twenty3" "thirty4" "forty4" "forty5"] [:!= :data "one"]]
           [["one" "twelve"]                                              [:in :data ["one" "twelve"]]]
           [[]                                                            [[:> :data "qux"] [:< :data "qux"]]]
           [["" "the one" "thirty4" "forty4" "forty5"]                    [[:!= :data "one"] [:!= :data "twelve"] [:!= :data "twenty3"]]]
           ]]
      (it (str "filters strings to " expected " with filters: " filters)
        (should== expected (datas (find :filters filters)))))

    (it "applies sorts"
      (should= [1 1 12 23 34 44 45] (intis (find :sorts [:inti :asc ]  :filters [:!= :inti nil])))
      (should= [45 44 34 23 12 1 1] (intis (find :sorts [:inti :desc ] :filters [:!= :inti nil])))
      (should= [44 45 1 1 34 12 23] (intis (find :sorts [:data :asc ]  :filters [:!= :data ""])))
      (should= [23 12 34 1 1 45 44] (intis (find :sorts [:data :desc ] :filters [:!= :data ""])))
      (should= ["one" "the one" "twelve" "twenty3" "thirty4" "forty4" "forty5"] (map :data (find-by-kind "testing" :sorts [[:inti :asc ] [:data :asc ]] :filters [:!= :inti nil])))
      (should= [44 45 1 1 34 12 23] (intis (find-by-kind "testing" :sorts [[:data :asc ] [:inti :asc ]] :filters [:!= :data ""]))))

    (it "applies limit and offset"
      (should= [12 23 34 44 45] (intis (find :sorts [:inti :asc ] :offset 2 :filters [:!= :inti nil])))
      (should= [12 23] (intis (find :sorts [:inti :asc ] :limit 2 :offset 2 :filters [:!= :inti nil])))
      (should= [34 44] (intis (find :sorts [:inti :asc ] :limit 2 :offset 4 :filters [:!= :inti nil])))
      (should= [45 44] (intis (find :sorts [:inti :desc ] :limit 2 :filters [:!= :inti nil])))
      (should= [34 23] (intis (find :sorts [:inti :desc ] :limit 2 :offset 2 :filters [:!= :inti nil])))
      (should= [12 1] (intis (find :sorts [:inti :desc ] :limit 2 :offset 4 :filters [:!= :inti nil]))))

    (it "applies formating to filters with dashes"
      (let [with-dashes (save {:kind "testing" :first-name "sam"})
            found-record (first (find :filters [[:= :first-name "sam"]]))]
        (should= "sam" (:first-name found-record)))))
  )

(defn save-testing-data []
  (save*
    {:kind "testing" :inti 1   :data "one"}
    {:kind "testing" :inti 12  :data "twelve"}
    {:kind "testing" :inti 23  :data "twenty3"}
    {:kind "testing" :inti 34  :data "thirty4"}
    {:kind "testing" :inti 45  :data "forty5"}
    {:kind "testing" :inti 1   :data "the one"}
    {:kind "testing" :inti nil :data ""}
    {:kind "testing" :inti 44  :data "forty4"}))

(defn save-other-testing-data []
  (save*
    {:kind "other-testing" :inti 56 :data "fifty6"}
    {:kind "other-testing" :inti 20 :data "twenty"}))

(defn it-searches-multiple-kinds []
  (list
    (before
      (save-testing-data)
      (save-other-testing-data))

    (it "finds by all kinds"
      (should= [1 1 12 20 23 34 44 45 56 nil] (map :inti (find-all-kinds :sorts [[:inti :asc ]])))
      (should= [1 1 12 20] (map :inti (find-all-kinds :sorts [[:inti :asc ]] :limit 4)))
      (should= [23 34 44 45 56 nil] (map :inti (find-all-kinds :sorts [[:inti :asc ]] :offset 4)))
      (should= [44 45] (map :inti (find-all-kinds :offset 6 :limit 2 :sorts [[:inti :asc ]])))
      (should= [34 44 45 56] (map :inti (find-all-kinds :filters [:> :inti 30] :sorts [:inti :asc ]))))

    (it "finds by kind"
      (let [records (find-by-kind "testing")]
        (should= 8 (count records))
        (should (every? #(= "testing" (:kind %)) records))))

    (it "counts by all kinds"
      (should= 10 (count-all-kinds))
      (should= 4 (count-all-kinds :filters [:> :inti 30] :sorts [:inti :asc ])))

    ))

(defn it-finds-by-kind []
  (list
    (before
      (save-testing-data))

    (it-can-find (fn [& args] (apply find-by-kind "testing" args)) "find-by-kind")

  ))

(defn it-finds-all-kinds []
  (list
    (before
      (save-testing-data))

    (it-can-find find-all-kinds "find-all-kinds")

    ))

(defn it-finds-by-key []
  (list
    (before
      (save-testing-data))

    (it "finds the items by key"
      (let [one (save {:kind "testing" :data "my stuff"})
            found (find-by-key (:key one))]
        (should= "my stuff" (:data found))
        (should= "testing" (:kind found))
        (should= (:key one) (:key found))))

    (it "returns nil for an invalid non existant key"
      (should-be-nil (find-by-key "blah")))

    (it "returns nil for an valid non existant key"
      (should-be-nil (find-by-key (compose-key "unknown-kind" 1))))

  ))

(defn it-counts-by-kind []
  (list
    (before
      (save-testing-data))

    (it "counts by kind"
      (should= 8 (count-by-kind "testing"))
      (should= 4 (count-by-kind "testing" :filters [:> :inti 20])))
  ))

