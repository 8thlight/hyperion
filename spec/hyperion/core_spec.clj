(ns hyperion.core-spec
  (:use [speclj.core]
        [hyperion.core]))

(describe "Datastore"

  (it "knows if a record is new"
    (should= true (new? {:kind "new"}))
    (should= false (new? {:kind "old" :key "exists"})))

  (it "resolves keys"
    (should= "abc" (->key "abc"))
    (should= "xyz" (->key {:key "xyz"})))

  (it "throws error when saving a record without a kind")
  (it "can find-by-key when given a record")
  (it "can delete when given a record")
  (it "reloads a record")

  )