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

  (it "has no ds by default"
    (reset! DS nil)
    (should-throw Exception "No Datastore bound (hyperion/*ds*) or installed (hyperion/DS)."
      (ds)))

  (it "the ds can be bound"
    (let [fake-ds (new-fake-datastore)]
      (binding [*ds* fake-ds]
        (should= fake-ds (ds)))))

  (it "the ds can be installed"
    (let [fake-ds (new-fake-datastore)]
      (try
        (reset! DS fake-ds)
        (should= fake-ds (ds))
        (finally (reset! DS nil)))))

  (it "knows if a record is new"
    (should= true (new? {:kind "new"}))
    (should= false (new? {:kind "old" :key "exists"})))

  (it "resolves keys"
    (should= "abc" (->key "abc"))
    (should= "xyz" (->key {:key "xyz"})))

  (it "translates filters"
    (should= := (#'hyperion.core/->filter-operator :=))
    (should= := (#'hyperion.core/->filter-operator "="))
    (should= := (#'hyperion.core/->filter-operator "eq"))
    (should= :!= (#'hyperion.core/->filter-operator :!=))
    (should= :!= (#'hyperion.core/->filter-operator "not"))
    (should= :> (#'hyperion.core/->filter-operator :>))
    (should= :> (#'hyperion.core/->filter-operator "gt"))
    (should= :< (#'hyperion.core/->filter-operator :<))
    (should= :< (#'hyperion.core/->filter-operator "lt"))
    (should= :>= (#'hyperion.core/->filter-operator :>=))
    (should= :>= (#'hyperion.core/->filter-operator "gte"))
    (should= :<= (#'hyperion.core/->filter-operator :<=))
    (should= :<= (#'hyperion.core/->filter-operator "lte"))
    (should= :contains? (#'hyperion.core/->filter-operator :contains?))
    (should= :contains? (#'hyperion.core/->filter-operator "contains?"))
    (should= :contains? (#'hyperion.core/->filter-operator "in"))
    (should-throw Exception "Unknown filter operator: foo" (#'hyperion.core/->filter-operator "foo")))

  (it "translates sort directions"
    (should= :asc (#'hyperion.core/->sort-direction :asc))
    (should= :asc (#'hyperion.core/->sort-direction :ascending))
    (should= :asc (#'hyperion.core/->sort-direction "asc"))
    (should= :asc (#'hyperion.core/->sort-direction "ascending"))
    (should= :desc (#'hyperion.core/->sort-direction :desc))
    (should= :desc (#'hyperion.core/->sort-direction :descending))
    (should= :desc (#'hyperion.core/->sort-direction "desc"))
    (should= :desc (#'hyperion.core/->sort-direction "descending"))
    (should-throw Exception "Unknown sort direction: foo" (#'hyperion.core/->sort-direction "foo")))

  (context "with fake datastore"

    (with _ds (new-fake-datastore))
    (before (reset! DS @_ds))

    (it "can find-by-key when given a record"
      (find-by-key {:key "some-key"})
      (check-call @_ds "ds-find-by-key" "some-key"))

    (it "can delete when given a record"
      (delete {:key "some-key"})
      (check-call @_ds "ds-delete" ["some-key"]))

    (it "reloads a record"
      (reload {:key "some-key"})
      (check-call @_ds "ds-find-by-key" "some-key"))

    (it "saves records with values as options"
      (save {:kind "one"} :value 42)
      (let [[call params] (first @(.calls @_ds))]
        (should= "ds-save" call)
        (should= 42 (:value (first params)))))

    (it "handles simple find-by-kind"
      (find-by-kind "thing")
      (check-call @_ds "ds-find-by-kind" "thing" nil nil nil nil))

    (it "translates filters"
      (find-by-kind "thing" :filters [:= :a :b])
      (check-call @_ds "ds-find-by-kind" "thing" [[:= :a :b]] nil nil nil)
      (find-by-kind "thing" :filters [["=" :a :b] ["eq" :x :y]])
      (check-call @_ds "ds-find-by-kind" "thing" [[:= :a :b] [:= :x :y]] nil nil nil))

    (it "translates sorts"
      (find-by-kind "thing" :sorts [:a :asc])
      (check-call @_ds "ds-find-by-kind" "thing" nil [[:a :asc]] nil nil)
      (find-by-kind "thing" :sorts [[:a "asc"] [:b :descending]])
      (check-call @_ds "ds-find-by-kind" "thing" nil [[:a :asc] [:b :desc]] nil nil))

    (it "pass along limit and offset"
      (find-by-kind "thing" :limit 5 :offset 6)
      (check-call @_ds "ds-find-by-kind" "thing" nil nil 5 6))

    (it "can count-by-kind"
      (count-by-kind "thing" :filters ["eq" :a :b])
      (check-call @_ds "ds-count-by-kind" "thing" [[:= :a :b]]))

    (it "can find-all-kinds "
      (find-all-kinds :filters ["eq" :a :b] :sorts [:c "ascending"] :limit 32 :offset 43)
      (check-call @_ds "ds-find-all-kinds" [[:= :a :b]] [[:c :asc]] 32 43))

    (it "can count-all-kinds "
      (count-all-kinds :filters ["eq" :a :b])
      (check-call @_ds "ds-count-all-kinds" [[:= :a :b]]))

    )

  )