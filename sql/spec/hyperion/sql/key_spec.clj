(ns hyperion.sql.key-spec
  (:use
    [speclj.core]
    [hyperion.sql.key]))

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
      (should= ["account-one-two", 1] (destructure-key "account-one-two-1")))

    (context "improper data"
      (it "handles no dash"
        (should= ["something", nil] (destructure-key "something")))

      (it "handles nil"
        (should= ["", nil] (destructure-key nil)))

      (it "handles empty string"
        (should= ["", nil] (destructure-key ""))))))
