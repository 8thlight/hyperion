(ns hyperion.dev.spec.deleting
  (:require [speclj.core :refer :all ]
            [hyperion.api :refer [save save* find-by-kind find-by-key delete-by-key delete-by-kind]]
            [hyperion.key :refer [compose-key]]
            ))

(defn it-deletes-by-key []
  (list
    (it "deletes by key"
      (let [kind :testing one (save {:kind kind :name "jim"})
            key (:key one)]
        (delete-by-key key)
        (should= nil (find-by-key key))))

    (it "returns nil for an invalid non existant key"
      (should-be-nil (delete-by-key "blah")))

    (it "returns nil for an valid non existant key"
      (should-be-nil (delete-by-key (compose-key "unknown-kind" 1))))

    ))

(defn it-deletes-by-kind []
  (list
    (before
      (save*
        {:kind "testing" :inti 1  :data "one"}
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
        (should= [] result)))

    ))
