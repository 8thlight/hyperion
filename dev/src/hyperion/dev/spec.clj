(ns hyperion.dev.spec
  (:use
    [speclj.core]
    [hyperion.core]))

(defn it-saves []
  (context "save"
    (it "saves a map with kind as a string and returns it"
      (let [record (save {:kind "testing" :name "ann" :key nil})]
        (should= "testing" (:kind record))
        (should= "ann" (:name record))))

    (it "saves a map with kind as a symbol and returns it"
      (let [record (save {:kind :testing :name "ann"})]
        (should= "testing" (:kind record))
        (should= "ann" (:name record))))

    (it "it saves an existing record"
      (let [record1 (save {:kind "other-testing" :name "ann"})
            record2 (save (assoc record1 :name "james"))]
        (should= (:key record1) (:key record2))
        (should= 1 (count (find-by-kind "other-testing")))))

    (it "assigns key to new records"
      (let [record (save {:kind "testing" :name "ann"})]
        (should-not= nil (:key record))))

    (it "assigned keys are unique"
      (should= 10 (count (set (map #(:key (save {:kind "testing" :name %})) (range 10))))))

    (it "saves date types"
      (let [date-without-timestamp (java.util.Date. 50, 3, 20)
            record (save {:kind :testing :birthdate date-without-timestamp})]
        (should= date-without-timestamp (:birthdate record))
        (should= date-without-timestamp (:birthdate (find-by-key (:key record))))))

    (it "can save many records"
      (let [inf-records (map #(hash-map :kind "testing" :name (str %)) (iterate inc 0))
            saved (save* (take 10 inf-records))]
        (should= 10 (count (set (map :key saved))))
        (should= 10 (count (find-by-kind "testing")))
        (should= (map str (range 10)) (sort (map :name (find-by-kind "testing"))))))))

(defn it-deletes []
  (context "delete"
    (it "deletes records"
      (let [one (save {:kind "testing" :name "jim"})]
        (delete one)
        (should= nil (find-by-key (:key one)))))))

(defn it-can-be-searched []
  (context "searching"
    (before
      (save* [{:kind "testing" :inti 1  :data "one"}
              {:kind "testing" :inti 12 :data "twelve"}
              {:kind "testing" :inti 23 :data "twenty3"}
              {:kind "testing" :inti 34 :data "thirty4"}
              {:kind "testing" :inti 45 :data "forty5"}
              {:kind "testing" :inti 1 :data "the one"}
              {:kind "testing" :inti 44 :data "forty4"}]))

    (let [find-fns [{
                     :fn find-all-kinds
                     :name "find-all-kinds"
                    }
                    {
                     :fn (fn [& args] (apply find-by-kind "testing" args))
                     :name "find-by-kind"
                    }]]
      (for [find-fn find-fns]
        (let [find (:fn find-fn)]
          (context (:name find-fn)
            (it "returns the kind"
              (should (every? #(= "testing" (:kind %)) (find))))

            (it "applies filters to with ints"
              (should= #{1 12 23} (set (map #(int (:inti %)) (find :filters [:< :inti 25]))))
              (should= #{1 12 23} (set (map #(int (:inti %)) (find :filters [:<= :inti 23]))))
              (should= #{34 44 45} (set (map #(int (:inti %)) (find :filters [:> :inti 25]))))
              (should= #{34 44 45} (set (map #(int (:inti %)) (find :filters [:>= :inti 34]))))
              (should= #{34} (set (map #(int (:inti %)) (find :filters [:= :inti 34]))))
              (should= #{1 12 23 44 45} (set (map #(int (:inti %)) (find :filters [:!= :inti 34]))))
              (should= #{12 34} (set (map #(int (:inti %)) (find :filters [:in :inti [12 34]]))))
              (should= #{12 34} (set (map #(int (:inti %)) (find :filters [:contains? :inti [12 34]]))))
              (should= #{} (set (map #(int (:inti %)) (find :filters [[:< :inti 24] [:> :inti 25]]))))
              (should= #{1 44 45} (set (map #(int (:inti %)) (find :filters [[:!= :inti 12] [:!= :inti 23] [:!= :inti 34]]))))
              (should= #{1 44 45} (set (map #(int (:inti %)) (find :filters [[:not :inti 12] [:not :inti 23] [:not :inti 34]])))))

            (it "applies filters to with strings"
              (should= #{"one" "forty4" "forty5"} (set (map :data (find :filters [:< :data "qux"]))))
              (should= #{"one" "forty4" "forty5"} (set (map :data (find :filters [:<= :data "one"]))))
              (should= #{"the one" "twelve" "twenty3" "thirty4"} (set (map :data (find :filters [:> :data "qux"]))))
              (should= #{"twelve" "twenty3" "thirty4"} (set (map :data (find :filters [:>= :data "thirty4"]))))
              (should= #{"one"} (set (map :data (find :filters [:= :data "one"]))))
              (should= #{"the one" "twelve" "twenty3" "thirty4" "forty4" "forty5"} (set (map :data (find :filters [:!= :data "one"]))))
              (should= #{"one" "twelve"} (set (map :data (find :filters [:in :data ["one" "twelve"]]))))
              (should= #{} (set (map :data (find :filters [[:> :data "qux"] [:< :data "qux"]]))))
              (should= #{"the one" "thirty4"  "forty4" "forty5"} (set (map :data (find :filters [[:!= :data "one"] [:!= :data "twelve"] [:!= :data "twenty3"]])))))

            (it "applies sorts"
              (should= [1 1 12 23 34 44 45] (map :inti (find :sorts [:inti :asc])))
              (should= [45 44 34 23 12 1 1] (map :inti (find :sorts [:inti :desc])))
              (should= [44 45 1 1 34 12 23] (map :inti (find :sorts [:data :asc])))
              (should= [23 12 34 1 1 45 44] (map :inti (find :sorts [:data :desc])))
              (should= ["one" "the one" "twelve" "twenty3" "thirty4" "forty4" "forty5"] (map :data (find-by-kind "testing" :sorts [[:inti :asc] [:data :asc]])))
              (should= [44 45 1 1 34 12 23] (map :inti (find-by-kind "testing" :sorts [[:data :asc] [:inti :asc]]))))

            (it "sorts treat nil as last"
              (let [no-inti (save {:kind "testing" :inti nil :data "mo"})
                    no-data (save {:kind "testing" :inti 25 :data nil})]
                (should= no-inti (last (find :sorts [:inti :asc])))
                (should= no-inti (first (find :sorts [:inti :desc])))
                (should= no-data (last (find :sorts [:data :asc])))
                (should= no-data (first (find :sorts [:data :desc])))))

            (it "applies limit and offset"
              (should= [12 23 34 44 45] (map :inti (find :sorts [:inti :asc] :offset 2)))
              (should= [12 23] (map :inti (find :sorts [:inti :asc] :limit 2 :offset 2)))
              (should= [34 44] (map :inti (find :sorts [:inti :asc] :limit 2 :offset 4)))
              (should= [45 44] (map :inti (find :sorts [:inti :desc] :limit 2)))
              (should= [34 23] (map :inti (find :sorts [:inti :desc] :limit 2 :offset 2)))
              (should= [12 1] (map :inti (find :sorts [:inti :desc] :limit 2 :offset 4))))

            (it "applies formating to filters with dashes"
              (let [with-dashes (save {:kind "testing" :first-name "sam"})
                    found-record (first (find :filters [[:= :first-name "sam"]]))]
                (should= with-dashes found-record)
                (should= "sam" (:first-name found-record))))))))

    (it "counts by kind"
      (should= 7 (count-by-kind "testing"))
      (should= 4 (count-by-kind "testing" :filters [:> :inti 20])))

    (it "finds the items by key"
      (let [one (save {:kind "testing" :data "my stuff"})]
        (should= one (find-by-key (:key one)))))

    (it "returns nil for bad input to find-by-key"
      (should= nil (find-by-key nil))
      (should= nil (find-by-key ""))
      (should= nil (find-by-key "asldjfa"))
      (should= [] (find-by-kind "asldfjasd")))

    (it "finds the items by keys"
      (let [one (save {:kind "testing" :data "my stuff"})
            two (save {:kind "testing" :data "my other stuff"})]
        (should= [one two] (find-by-keys [(:key one) (:key two)]))))

    (context "with multiple kinds"
      (before
        (save* [{:kind "other-testing" :inti 56 :data "fifty6"}
                {:kind "other-testing" :inti 20 :data "twenty"}]))

      (it "finds by all kinds (find-all-kinds)"
        (should= [1 1 12 20 23 34 44 45 56] (map :inti (find-all-kinds :sorts [[:inti :asc]])))
        (should= [1 1 12 20] (map :inti (find-all-kinds :sorts [[:inti :asc]] :limit 4)))
        (should= [23 34 44 45 56] (map :inti (find-all-kinds :sorts [[:inti :asc]] :offset 4)))
        (should= [44 45] (map :inti (find-all-kinds :offset 6 :limit 2 :sorts [[:inti :asc]])))
        (should= [34 44 45 56] (map :inti (find-all-kinds :filters [:> :inti 30] :sorts [:inti :asc]))))

      (it "counts by all kinds (count-all-kinds)"
        (should= 9 (count-all-kinds))
        (should= 4 (count-all-kinds :filters [:> :inti 30] :sorts [:inti :asc])))

      (it "finds all records by kind"
        (let [records (find-by-kind "testing")]
          (should= 7 (count records))
          (should (every? #(= "testing" (:kind %)) records)))))))

(defn it-behaves-like-a-datastore []
  (list
    (it-saves)
    (it-deletes)
    (it-can-be-searched)))
