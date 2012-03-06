(ns hyperion.core-spec
  (:use [speclj.core]
        [hyperion.core]
        [hyperion.fake]))

(defmacro check-call [ds name & params]
  `(let [call# (first @(.calls ~ds))]
     (swap! (.calls ~ds) rest)
     (should= ~name (first call#))
     (should= '~params (second call#))))

(describe "Datastore Core"

  (it "knows if a record is new"
    (should= true (new? {:kind "new"}))
    (should= false (new? {:kind "old" :key "exists"})))

  (it "resolves keys"
    (should= "abc" (->key "abc"))
    (should= "xyz" (->key {:key "xyz"})))

  (it "throws error when saving a record without a kind"
    (should-throw Exception "Can't save record without a :kind"
      (save {:wazzup "I'm kindless"}))

  (it "throws error when saving any of many record without a kind"
    (should-throw Exception "Can't save record without a :kind"
      (save* {:kind "ok"} {:wazzup "I'm kindless"})))
    )

  (context "with fake datastore"

    (with ds (new-fake-datastore))
    (before (reset! DS @ds))

    (it "can find-by-key when given a record"
      (find-by-key {:key "some-key"})
      (check-call @ds "ds-find-by-key" "some-key"))

    (it "can delete when given a record"
      (delete {:key "some-key"})
      (check-call @ds "ds-delete" ["some-key"]))

    (it "reloads a record"
      (reload {:key "some-key"})
      (check-call @ds "ds-find-by-key" "some-key"))

    (it "saves records with values as options"
      (save {:kind "one"} :value 42)
      (let [[call params] (first @(.calls @ds))]
        (should= "ds-save" call)
        (should= 42 (:value (first params)))))

    )

  )