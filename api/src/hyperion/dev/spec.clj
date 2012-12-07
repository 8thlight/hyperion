(ns hyperion.dev.spec
  (:require [speclj.core :refer :all ]
            [speclj.util :refer [endl]]
            [hyperion.types :refer [foreign-key]]
            [hyperion.api :refer :all ]
            [hyperion.key :refer [compose-key]]
            [hyperion.abstr :refer :all ])
  (:import  [speclj SpecFailure]))

;;;; should=coll matcher (until it gets added to speclj) ;;;;

(defn coll-includes? [coll item]
  (some #(= % item) coll))

(defn remove-first [coll value]
  (loop [coll coll seen []]
    (if (empty? coll)
      seen
      (let [f (first coll)]
        (if (= f value)
          (concat seen (rest coll))
          (recur (rest coll) (conj seen f)))))))

(defn coll-difference [coll1 coll2]
  (loop [match-with coll1 match-against coll2 diff []]
    (if (empty? match-with)
      diff
      (let [f (first match-with)
            r (rest match-with)]
        (if (coll-includes? match-against f)
          (recur r (remove-first match-against f) diff)
          (recur r match-against (conj diff f)))))))

(defn difference-message [expected actual extra missing]
  (-> (str "Expected collection contained:  " (prn-str expected) endl "Actual collection contained:    " (prn-str actual))
    (#(if (empty? missing)
       %
       (str % endl "The missing elements were:      " (prn-str missing))))
    (#(if (empty? extra)
       %
       (str % endl "The extra elements were:        " (prn-str extra))))
    ))

(defmacro should=coll
  "Asserts that the collection is contained within another collection"
  [expected actual]
  `(let [extra# (coll-difference ~actual ~expected)
         missing# (coll-difference ~expected ~actual)]
     (if-not (and (empty? extra#) (empty? missing#))
       (let [error-message# (difference-message ~expected ~actual extra# missing#)]
         (throw (SpecFailure. error-message#))))
     )
  )

;;;;

(defn it-saves []
  (context "save"
    (it "saves a map with kind as a string and returns it"
      (let [record (save {:kind "testing" :name "ann"})]
        (should= "testing" (:kind record))
        (should= "ann" (:name record))))

    (it "saves a map with kind as a symbol and returns it"
      (let [record (save {:kind :testing :name "ann"})]
        (should= "testing" (:kind record))
        (should= "ann" (:name record))))

    (it "saves an existing record"
      (let [record1 (save {:kind "other-testing" :name "ann"})
            record2 (save (assoc record1 :name "james"))]
        (should= (:key record1) (:key record2))
        (should= 1 (count (find-by-kind "other-testing")))))

    (it "assigns an key to new records"
      (let [record (save {:kind "testing" :name "ann"})]
        (should-not= nil (:key record))))

    (it "assigned keys are unique"
      (should= 10 (count (set (map #(:key (save {:kind "testing" :name %})) (range 10))))))

    (it "can save many records"
      (let [inf-records (map #(hash-map :kind "testing" :name (str %)) (iterate inc 0))
            saved (apply save* (take 10 inf-records))]
        (should= 10 (count (set (map :key saved))))
        (should= 10 (count (find-by-kind "testing")))
        (should= (map str (range 10)) (sort (map :name (find-by-kind "testing"))))))

    (it "an empty record"
      (let [result (save {:kind "testing"})]
        (should= nil (:name (find-by-key (:key result))))))
    )
  )

(defn it-deletes []
  (list
    (it "deletes by key"
      (let [kind :testing one (save {:kind kind :name "jim"})
            key (:key one)]
        (delete-by-key key)
        (should= nil (find-by-key key))))

    (context "deletes by kind"
      (before
        (save* {:kind "testing" :inti 1 :data "one"}
          {:kind "testing" :inti 12 :data "twelve"}))

      (it "with no filters"
        (let [kind :testing ]
          (delete-by-kind kind)
          (should= [] (find-by-kind kind))))

      (it "with equal filter on an int"
        (let [kind :testing _ (delete-by-kind kind :filters [[:= :inti 1]])
              result (find-by-kind kind)]
          (should= 1 (count result))
          (should= 12 (:inti (first result)))))

      (it "with equal filter on an string"
        (let [kind :testing _ (delete-by-kind kind :filters [[:= :data "one"]])
              result (find-by-kind kind)]
          (should= 1 (count result))
          (should= 12 (:inti (first result)))))

      (it "with not equal to filter"
        (let [kind :testing _ (delete-by-kind kind :filters [[:!= :inti 1]])
              result (find-by-kind kind)]
          (should= 1 (count result))
          (should= 1 (:inti (first result)))))

      (it "with less than or equal to filter"
        (let [kind :testing _ (delete-by-kind kind :filters [[:<= :inti 1]])
              result (find-by-kind kind)]
          (should= 1 (count result))
          (should= 12 (:inti (first result)))))

      (it "with less than filter"
        (let [kind :testing _ (delete-by-kind kind :filters [[:<= :inti 2]])
              result (find-by-kind kind)]
          (should= 1 (count result))
          (should= 12 (:inti (first result)))))

      (it "with greater than or equal to filter"
        (let [kind :testing _ (delete-by-kind kind :filters [[:>= :inti 2]])
              result (find-by-kind kind)]
          (should= 1 (count result))
          (should= 1 (:inti (first result)))))

      (it "with greater than filter"
        (let [kind :testing _ (delete-by-kind kind :filters [[:> :inti 1]])
              result (find-by-kind kind)]
          (should= 1 (count result))
          (should= 1 (:inti (first result)))))

      (it "with contains filter"
        (let [kind :testing _ (delete-by-kind kind :filters [[:in :inti [1]]])
              result (find-by-kind kind)]
          (should= 1 (count result))
          (should= 12 (:inti (first result)))))

      (it "with contains filter"
        (let [kind :testing _ (delete-by-kind kind :filters [[:in :inti [1 12]]])
              result (find-by-kind kind)]
          (should= 0 (count result))
          (should= [] result))))))

(defn it-can-find [find name]
  (context name
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
      (it (str "filters ints to " expected " with fitlers: " filters)
        (should=coll expected (intis (find :filters filters)))))

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
      (it (str "filters strings to " expected " with fitlers: " filters)
        (should=coll expected (datas (find :filters filters)))))

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
        (should= "sam" (:first-name found-record))))))

(defn it-can-be-searched []
  (context "searching"
    (before
      (save*
        {:kind "testing" :inti 1   :data "one"}
        {:kind "testing" :inti 12  :data "twelve"}
        {:kind "testing" :inti 23  :data "twenty3"}
        {:kind "testing" :inti 34  :data "thirty4"}
        {:kind "testing" :inti 45  :data "forty5"}
        {:kind "testing" :inti 1   :data "the one"}
        {:kind "testing" :inti nil :data ""}
        {:kind "testing" :inti 44  :data "forty4"}
        ))

    (it-can-find find-all-kinds "find-all-kinds")
    (it-can-find (fn [& args] (apply find-by-kind "testing" args)) "find-by-kind")

    (it "counts by kind"
      (should= 8 (count-by-kind "testing"))
      (should= 4 (count-by-kind "testing" :filters [:> :inti 20])))

    (context "find-by-key"
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

             )

    (context "with multiple kinds"
      (before
        (save* {:kind "other-testing" :inti 56 :data "fifty6"}
          {:kind "other-testing" :inti 20 :data "twenty"}))

      (it "finds by all kinds (find-all-kinds)"
        (should= [1 1 12 20 23 34 44 45 56 nil] (map :inti (find-all-kinds :sorts [[:inti :asc ]])))
        (should= [1 1 12 20] (map :inti (find-all-kinds :sorts [[:inti :asc ]] :limit 4)))
        (should= [23 34 44 45 56 nil] (map :inti (find-all-kinds :sorts [[:inti :asc ]] :offset 4)))
        (should= [44 45] (map :inti (find-all-kinds :offset 6 :limit 2 :sorts [[:inti :asc ]])))
        (should= [34 44 45 56] (map :inti (find-all-kinds :filters [:> :inti 30] :sorts [:inti :asc ]))))

      (it "counts by all kinds (count-all-kinds)"
        (should= 10 (count-all-kinds))
        (should= 4 (count-all-kinds :filters [:> :inti 30] :sorts [:inti :asc ])))

      (it "finds all records by kind"
        (let [records (find-by-kind "testing")]
          (should= 8 (count records))
          (should (every? #(= "testing" (:kind %)) records)))))))

(defentity OtherShirt
  [account-key :type (foreign-key :account ) :db-name :account-id])

(defentity Shirt
  [account-key :type (foreign-key :account ) :db-name :account-id])

(defentity TShirt ; configure to use the shirt table
  [account-key :type (foreign-key :account ) :db-name "account_id"])

(defentity Account
  [first-name])

(defn it-handles-keys []
  (context "keys"

    (it "saves records with foreign keys"
      (let [account (save {:kind :account})
            shirt (save (shirt :account-key (:key account)))
            found-shirt (find-by-key (:key shirt))
            found-account (find-by-key (:key account))
            account-key (:key account)]
        (should= account-key (:account-key shirt))
        (should= account-key (:account-key found-shirt))
        (should= account-key (:key found-account))))

    (it "saves records with foreign keys and db-name as string with underscore"
      (pending "pending configuring table name so I don't need to create a new table")
      (let [account (save (account))
            shirt (save (tshirt :account-key (:key account)))
            account-key (:key account)]
        (should= account-key (:account-key shirt))))

    (it "handles bad keys in filters"
      (should= [] (find-by-kind :shirt :filters [:= :account-key (compose-key "account" 321)])))

    (it " can be filtered by value"
      (let [account (save (account))
            shirt (save (shirt :account-key (:key account)))]
        (should= [shirt] (find-by-kind :shirt :filters [:= :account-key (:key account)]))))

    (it " can be filtered by value"
      (let [shirt (save {:kind :shirt :account-key nil})]
        (should= [shirt] (find-by-kind :shirt :filters [:= :account-key nil]))))

    ))

(defn it-behaves-like-a-datastore []
  (list
    (it-saves)
    (it-deletes)
    (it-can-be-searched)
    (it-handles-keys)
    ))

(run-specs)
