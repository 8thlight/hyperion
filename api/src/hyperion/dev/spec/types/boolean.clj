(ns hyperion.dev.spec.types.boolean
  (:require [speclj.core :refer :all ]
            [hyperion.api :refer [save save* find-by-kind find-by-key]]))

(defn it-handles-booleans []
  (list

    (context "saving"
      (it "saves true"
        (let [record (save {:kind :types :bool true})]
          (should= true (:bool (find-by-key (:key record))))))

      (it "saves false"
        (let [record (save {:kind :types :bool false})]
          (should= false (:bool (find-by-key (:key record))))))

      (it "saves nil"
        (let [record (save {:kind :types :bool nil})]
          (should-be-nil (:bool (find-by-key (:key record))))))

      )

    (context "find"
      (before
        (save*
          {:kind :types :bool true}
          {:kind :types :bool false}
          {:kind :types :bool nil}))

      (defn result-count [value]
        (count (find-by-kind :types :filters [:= :bool value])))

      (it "finds true"
        (should= 1 (result-count true)))

      (it "finds false"
        (should= 1 (result-count false)))

      (it "finds nil"
        (should= 1 (result-count nil))))

    ))
