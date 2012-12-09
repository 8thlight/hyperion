(ns hyperion.dev.spec.foreign-keys
  (:require [speclj.core :refer :all ]
            [hyperion.api :refer [defentity save find-by-key find-by-kind count-by-kind delete-by-kind]]
            [hyperion.types :refer [foreign-key]]
            [hyperion.key :refer [compose-key]]))

(defentity OtherShirt
  [account-key :type (foreign-key :account ) :db-name :account-id])

(defentity Shirt
  [account-key :type (foreign-key :account ) :db-name :account-id])

(defentity TShirt ; configure to use the shirt table
  [account-key :type (foreign-key :account ) :db-name "account_id"])

(defentity Account
  [first-name])

(defn it-handles-foreign-keys []
  (list
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

    (it "handles bad keys in find filters"
      (let [account (save (account))
            account-key (:key account)
            shirt (save (shirt :account-key account-key))
            other-shirt (save {:kind :shirt})]
        (should= [] (find-by-kind :shirt :filters [:= :account-key (compose-key "account" 321)]))
        (should= [] (find-by-kind :shirt :filters [:= :account-key "blah"]))
        (should= [shirt] (find-by-kind :shirt :filters [:in :account-key [account-key (compose-key "account" 321)]]))
        (should= [shirt] (find-by-kind :shirt :filters [:in :account-key [account-key "blah"]]))))

    (it "handles bad keys in count filters"
      (let [account (save (account))
            account-key (:key account)
            shirt (save (shirt :account-key account-key))
            other-shirt (save {:kind :shirt})]
        (should= 0 (count-by-kind :shirt :filters [:= :account-key (compose-key "account" 321)]))
        (should= 0 (count-by-kind :shirt :filters [:= :account-key "blah"]))
        (should= 1 (count-by-kind :shirt :filters [:in :account-key [account-key (compose-key "account" 321)]]))
        (should= 1 (count-by-kind :shirt :filters [:in :account-key [account-key "blah"]]))))

    (it "handles bad keys in delete filters"
      (let [account (save (account))
            account-key (:key account)
            other-account (save {:kind :account})
            shirt (save (shirt :account-key account-key))
            other-shirt (save {:kind :shirt :account-key (:key other-account)})]
        (should-be-nil (delete-by-kind :shirt :filters [:= :account-key (compose-key "account" 321)]))
        (should= 2 (count-by-kind :shirt))
        (should-be-nil (delete-by-kind :shirt :filters [:= :account-key "blah"]))
        (should= 2 (count-by-kind :shirt))
        (should-be-nil (delete-by-kind :shirt :filters [:in :account-key [account-key (compose-key "account" 321)]]))
        (should= 1 (count-by-kind :shirt))
        (should-be-nil (delete-by-kind :shirt :filters [:in :account-key [(:key other-account) "blah"]]))
        (should= 0 (count-by-kind :shirt))))

    (it "can be filtered by value"
      (let [account (save (account))
            shirt (save (shirt :account-key (:key account)))]
        (should= [shirt] (find-by-kind :shirt :filters [:= :account-key (:key account)]))))

    (it "can be filtered by value"
      (let [shirt (save {:kind :shirt :account-key nil})]
        (should= [shirt] (find-by-kind :shirt :filters [:= :account-key nil]))))

    ))
