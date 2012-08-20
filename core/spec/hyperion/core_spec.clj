(ns hyperion.core-spec
  (:use [speclj.core]
        [hyperion.core]
        [hyperion.filtering :only [make-filter]]
        [hyperion.sorting :only [make-sort]]
        [hyperion.fake]
        [clojure.string :only (upper-case)]
        [chee.datetime :only [now before? seconds-ago between? seconds-from-now]]))

(defn check-call [ds index name params]
  (let [call (get @(.calls ds) index)]
    (should= name (first call))
    (should= params (second call))))

(defn check-first-call [ds name & params]
  (check-call ds 0 name params))

(defn check-second-call [ds name & params]
  (check-call ds 1 name params))

(defn check-third-call [ds name & params]
  (check-call ds 2 name params))

(def supported-filters
  [[:= := ]
   ["=" := ]
   ["eq" := ]
   [:!= :!= ]
   ["!=" :!= ]
   ["not" :!= ]
   [:> :> ]
   [">" :> ]
   ["gt" :> ]
   [:< :< ]
   ["<" :< ]
   ["lt" :< ]
   [:>= :>= ]
   [">=" :>= ]
   ["gte" :>= ]
   [:<= :<= ]
   ["<=" :<= ]
   ["lte" :<= ]
   [:contains? :contains? ]
   ["contains?" :contains? ]
   [:contains :contains? ]
   ["contains" :contains? ]
   [:in? :contains? ]
   ["in?" :contains? ]
   [:in :contains? ]
   ["in" :contains? ]])

(defn for-filters [test-fn]
  (for [[filter normalized-filter] supported-filters]
    (test-fn filter normalized-filter)))

(defn it-parses-filters [call-fn assert-fn]
  (list
    (for-filters
      (fn [filter normalized-filter]
        (it (str "converts operator " filter " to " normalized-filter)
          (let [filter-struct (make-filter normalized-filter :key 1)]
            (call-fn "unknown" :filters [filter :key 1])
            (assert-fn "unknown" [filter-struct])))))

    (it "throws for unknown filter"
      (should-throw Exception (call-fn "unknown" :filters [:foo :key 1])))

    (it "converts the field to a keyword"
      (let [filter-struct (make-filter := :key 1)]
        (call-fn "unknown" :filters [:= "key" 1])
        (assert-fn "unknown" [filter-struct])))

    (it "packs values"
      (let [filter-struct (make-filter := :widget 1)]
        (call-fn "packable" :filters [:= :widget "1"])
        (assert-fn "packable" [filter-struct])))

    (it "handles multiple filters"
      (let [filter-struct (make-filter := :key 1)
            second-filter (make-filter :!= :key 2)]
        (call-fn "unknown" :filters [[:= :key 1] [:!= :key 2]])
        (assert-fn "unknown" [filter-struct second-filter])))))

(defn it-calls-by-kind [call-fn assert-fn]
  (list
    (it "with kind as string"
      (call-fn "one")
      (assert-fn "one" []))

    (it "with kind as keyword"
      (call-fn :one )
      (assert-fn "one" []))

    (context "filters"
      (it-parses-filters call-fn assert-fn))))

(defentity Hollow)

(defentity OneField
  [field])

(defentity :many-fields [field1]
  [field2]
  [field42])

(defn this-fn [here] here)

(defentity :many-defaulted-fields [field1]
  [field2]
  [field3 :default ".141592" :packer this-fn]
  [field42 :default "value42" :packer #(apply str (reverse %))])

(defentity "PACKABLE"
  [widget :type Integer]
  [bauble :packer #(apply str (reverse %)) :unpacker #(if % (upper-case %) %)]
  [thingy :unpacker true])

(defmethod pack Integer [_ value]
  (when value
    (Integer. (Integer/parseInt value))))

(defmethod unpack Integer [_ value]
  (when value
    (str value)))

(defentity Hooks
  [field])

(defmethod after-create :hooks [record]
  (assoc record :create-message (str "created with: " (:field record))))

(defmethod before-save :hooks [record]
  (assoc record :save-message (str "saving with: " (:field record))))

(defmethod after-load :hooks [record]
  (assoc record :load-message (str "loaded with: " (:field record))))

(defentity Timestamps
  [created-at]
  [updated-at])

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

  (it "translates filters"
    (should= := (#'hyperion.core/->filter-operator := ))
    (should= := (#'hyperion.core/->filter-operator "="))
    (should= := (#'hyperion.core/->filter-operator "eq"))
    (should= :!= (#'hyperion.core/->filter-operator :!= ))
    (should= :!= (#'hyperion.core/->filter-operator "not"))
    (should= :> (#'hyperion.core/->filter-operator :> ))
    (should= :> (#'hyperion.core/->filter-operator "gt"))
    (should= :< (#'hyperion.core/->filter-operator :< ))
    (should= :< (#'hyperion.core/->filter-operator "lt"))
    (should= :>= (#'hyperion.core/->filter-operator :>= ))
    (should= :>= (#'hyperion.core/->filter-operator "gte"))
    (should= :<= (#'hyperion.core/->filter-operator :<= ))
    (should= :<= (#'hyperion.core/->filter-operator "lte"))
    (should= :contains? (#'hyperion.core/->filter-operator :contains? ))
    (should= :contains? (#'hyperion.core/->filter-operator "contains?"))
    (should= :contains? (#'hyperion.core/->filter-operator "in"))
    (should-throw Exception "Unknown filter operator: foo" (#'hyperion.core/->filter-operator "foo")))

  (it "translates sort directions"
    (should= :asc (#'hyperion.core/->sort-direction :asc ))
    (should= :asc (#'hyperion.core/->sort-direction :ascending ))
    (should= :asc (#'hyperion.core/->sort-direction "asc"))
    (should= :asc (#'hyperion.core/->sort-direction "ascending"))
    (should= :desc (#'hyperion.core/->sort-direction :desc ))
    (should= :desc (#'hyperion.core/->sort-direction :descending ))
    (should= :desc (#'hyperion.core/->sort-direction "desc"))
    (should= :desc (#'hyperion.core/->sort-direction "descending"))
    (should-throw Exception "Unknown sort direction: foo" (#'hyperion.core/->sort-direction "foo")))

  (context "with fake ds"
    (around [it]
      (binding [*ds* (new-fake-datastore)]
        (it)))

    (context "save"
      (it "saves records"
        (save {:kind "one" :value 42})
        (check-first-call (ds) "ds-save" [{:kind "one" :value 42}]))

      (it "saves records with values as options"
        (save {:kind "one"} :value 42)
        (check-first-call (ds) "ds-save" [{:kind "one" :value 42}]))

      (it "saves records with values as a map"
        (save {:kind "one"} {:field "kia" :value 42})
        (check-first-call (ds) "ds-save" [{:kind "one" :value 42 :field "kia"}]))

      (it "saves multiple entities at once"
        (save* {:kind "one" :value 42} {:kind "two" :value 35})
        (check-first-call (ds) "ds-save" [{:kind "one" :value 42} {:kind "two" :value 35}]))

      (it "packs entities"
        (save {:kind :packable :widget "42"})
        (check-first-call (ds) "ds-save" [{:kind "packable" :widget 42 :thingy nil :bauble ""}]))

      (it "converts the kind to a string"
        (save {:kind :one})
        (check-first-call (ds) "ds-save" [{:kind "one"}])))

    (it "delete by key"
      (delete-by-key "one42")
      (check-first-call (ds) "ds-delete-by-key" "one42"))

    (context "delete by kind"
      (it-calls-by-kind
        delete-by-kind
        (fn [kind filters]
          (check-first-call (ds) "ds-delete-by-kind" kind filters))))

    (context "count by kind"
      (it-calls-by-kind
        count-by-kind
        (fn [kind filters]
          (check-first-call (ds) "ds-count-by-kind" kind filters))))

    (it "reloads"
      (let [response {:thing 1}]
        (reset! (.responses (ds)) [response])
        (should= response (reload {:kind "kind" :key 1}))
        (check-first-call (ds) "ds-find-by-key" 1)))

    (it "find by key"
      (find-by-key "one42")
      (check-first-call (ds) "ds-find-by-key" "one42"))

    (context "find by kind"
      (it "finds with only a kind"
        (let [response [{:thing 1}]]
          (reset! (.responses (ds)) [response])
          (should= response (find-by-kind "hollow"))
          (check-first-call (ds) "ds-find-by-kind" "hollow" [] [] nil nil)))

      (context "filters"
        (it-parses-filters
          find-by-kind
          (fn [kind filters]
            (check-first-call (ds) "ds-find-by-kind" kind filters [] nil nil))))

      (context "sorts"
        (for [[order normalized-order]
              [[:asc :asc ]
               ["asc" :asc ]
               ["ascending" :asc ]
               [:desc :desc ]
               ["desc" :desc ]
               ["descending" :desc ]]]

          (it (str "converts order " order " to " normalized-order)
            (let [sort-struct (make-sort :name normalized-order)]
              (find-by-kind "unknown" :sorts [:name order])
              (check-first-call (ds) "ds-find-by-kind" "unknown" [] [sort-struct] nil nil))))

        (it "throws for unknown order"
          (should-throw Exception (find-by-kind "unknown" :sorts [:name :foo ])))

        (it "converts the field to a keyword"
          (let [sort-struct (make-sort :name :desc )]
            (find-by-kind "unknown" :sorts ["name" :desc ])
            (check-first-call (ds) "ds-find-by-kind" "unknown" [] [sort-struct] nil nil))))

      (it "passes the limit"
        (find-by-kind "unknown" :limit 1)
        (check-first-call (ds) "ds-find-by-kind" "unknown" [] [] 1 nil))

      (it "passes the offset"
        (find-by-kind "unknown" :offset 1)
        (check-first-call (ds) "ds-find-by-kind" "unknown" [] [] nil 1)))

    (context "find all kinds"
      (it "calls all kinds"
        (let [responses [["kind1" "kind2"] [] []]]
          (reset! (.responses (ds)) responses)
          (find-all-kinds)
          (check-first-call (ds) "ds-all-kinds")
          (check-second-call (ds) "ds-find-by-kind" "kind1" [] nil nil nil)
          (check-third-call (ds) "ds-find-by-kind" "kind2" [] nil nil nil))))

    (context "entities"
      (it "saves the fields defined in the entity"
        (let [unsaved {:kind "one-field" :field "field" :foo "foo"}]
          (save unsaved)
          (check-first-call (ds) "ds-save" [{:kind "one-field" :field "field"}])))

      (it "saves the key field if not defined on the entity"
        (save {:kind "one-field" :field "field" :key 1})
        (check-first-call (ds) "ds-save" [{:kind "one-field" :field "field" :key 1}]))

      (context "packing"
        (it "types"
          (save {:kind :packable :widget "42"})
          (check-first-call (ds) "ds-save" [{:kind "packable" :widget 42 :bauble "" :thingy nil}]))

        (it "custom functions"
          (save {:kind :packable :bauble "hello"})
          (check-first-call (ds) "ds-save" [{:kind "packable" :widget nil :bauble "olleh" :thingy nil}]))

        (it "applies default values and packs them"
          (save {:kind :many-defaulted-fields})
          (check-first-call (ds) "ds-save" [{:kind "many-defaulted-fields"
                                             :field1 nil
                                             :field2 nil
                                             :field3 ".141592"
                                             :field42 "24eulav"}])))

      (context "unpacking"
        (context "normalizes"
          (it "attribues for unknown kind"
            (reset! (.responses (ds)) [[{"KIND" "unknown" :some_WEIRD_Field :val}]])
            (should= {:kind "unknown" :some-weird-field :val} (save {})))

          (it "kind for unknown kind"
            (reset! (.responses (ds)) [[{:kind :UNknown}]])
            (should= {:kind "unknown"} (save {})))

          (it "attribues for known kind"
            (reset! (.responses (ds)) [[{"KIND" "packable" :BAuble "val"}]])
            (should= {:kind "packable" :bauble "VAL" :widget nil :thingy nil} (save {})))

          (it "kind for known kind"
            (reset! (.responses (ds)) [[{:kind :packable}]])
            (should= {:kind "packable" :bauble nil :widget nil :thingy nil} (save {}))))

        (it "types"
          (reset! (.responses (ds)) [[{:kind "packable" :widget 42}]])
          (should= {:kind "packable" :bauble nil :widget "42" :thingy nil} (save {})))

        (it "unknown kinds"
          (reset! (.responses (ds)) [[{:kind "unknown" :widget 42}]])
          (should= {:kind "unknown" :widget 42} (save {})))

        (it "custom functions"
          (reset! (.responses (ds)) [[{:kind "packable" :bauble "hello"}]])
          (should= {:kind "packable" :bauble "HELLO" :widget nil :thingy nil} (save {})))

        (it "does not apply default values"
          (reset! (.responses (ds)) [[{:kind "many-defaulted-fields"}]])
          (should= {:kind "many-defaulted-fields" :field1 nil :field2 nil :field3 nil :field42 nil} (save {})))

        (it "nil"
          (reset! (.responses (ds)) [[nil]])
          (should= nil (save {}))))

      (context "hooks"
        (context "after create"
          (it "known kind"
            (let [unsaved (hooks :field "waza!")]
              (should= "waza!" (:field unsaved))
              (should= "created with: waza!" (:create-message unsaved))))

          (it "unknown kind"
            (defmethod after-create :unknown-kind-after-create [record]
              (assoc record :my-cool-field :value ))
            (reset! (.responses (ds)) [[{:kind "unknown-kind-after-create"}]])
            (should= {:kind "unknown-kind-after-create" :my-cool-field :value} (save {}))))

        (it "has before save hook"
          (save (hooks :field "waza!"))
          (check-first-call (ds) "ds-save" [{:kind "hooks"
                                             :field "waza!"
                                             :create-message "created with: waza!"
                                             :save-message "saving with: waza!"}]))

        (it "has after load hook"
          (let [response [{:kind "hooks" :field "waza!"}]
                _ (reset! (.responses (ds)) [response])
                loaded (first (find-by-kind "hooks"))]
            (should= "loaded with: waza!" (:load-message loaded)))))

      (context "Timestamps"
        (it "are automatically populated on save if the entity contains created-at and updated-at"
          (let [mock-date (java.util.Date.)]
            (with-redefs [now (fn [] mock-date)]
              (save (timestamps))
              (check-first-call (ds) "ds-save" [{:kind "timestamps"
                                                 :created-at mock-date
                                                 :updated-at mock-date}]))))

        (it "are saved based on kind, not provided keys"
          (let [mock-date (java.util.Date.)]
            (with-redefs [now (fn [] mock-date)]
              (save {:kind :timestamps})
              (check-first-call (ds) "ds-save" [{:kind "timestamps"
                                                 :created-at mock-date
                                                 :updated-at mock-date}]))))

        (it "does not update existing created-at"
          (let [created-at (java.util.Date. 1988 11 1)
                mock-date (java.util.Date.)]
            (with-redefs [now (fn [] mock-date)]
              (save {:kind :timestamps :created-at created-at})
              (should-not= created-at mock-date)
              (check-first-call (ds) "ds-save" [{:kind "timestamps"
                                                 :created-at created-at
                                                 :updated-at mock-date}]))))

        (it "does update existing updated-at"
          (let [existing-date (java.util.Date. 1988 11 1)
                mock-date (java.util.Date.)]
            (with-redefs [now (fn [] mock-date)]
              (save {:kind :timestamps :created-at existing-date :updated-at existing-date})
              (check-first-call (ds) "ds-save" [{:kind "timestamps"
                                                 :created-at existing-date
                                                 :updated-at mock-date}]))))))))
