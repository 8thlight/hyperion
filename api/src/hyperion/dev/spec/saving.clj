(ns hyperion.dev.spec.saving
  (:require [speclj.core :refer :all ]
            [hyperion.api :refer [save find-by-kind find-by-key save*]]))

(defn it-saves []
  (list
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

    ))
