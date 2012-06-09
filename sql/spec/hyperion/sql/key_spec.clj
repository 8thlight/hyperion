(ns hyperion.sql.key-spec
  (:require
    [speclj.core :refer :all]
    [hyperion.sql.key :refer :all]))

(describe "Sql keys"
  (context "key building"
    (it "builds key with string"
      (should= "account-1" (build-key "account" 1)))

    (it "builds key with keyword"
      (should= "account-1" (build-key :account 1))))

  (context "key destructuring"
    (it "destructures one word table name"
      (should= ["account", 1] (destructure-key "account-1")))

    (it "destructures multiple word table name"
      (should= ["account-one-two", 1] (destructure-key "account-one-two-1")))))
