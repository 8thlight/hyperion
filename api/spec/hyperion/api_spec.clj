(ns hyperion.api-spec
  (:use [speclj.core]
        [hyperion.api]
        [hyperion.filtering :only [make-filter]]
        [hyperion.sorting :only [make-sort]]
        [hyperion.fake]
        [hyperion.types :only [foreign-key]]
        [clojure.string :only (upper-case)]
        [chee.datetime :only [now before? seconds-ago between? seconds-from-now]]))

(defmacro check-call [ds index name params]
  `(let [call# (get @(.calls ~ds) ~index)]
     (should= ~name (first call#))
     (should= (seq ~params) (second call#))))

(defmacro check-first-call [ds name & params]
  `(check-call ~ds 0 ~name ~(vec params)))

(defmacro check-second-call [ds name & params]
  `(check-call ~ds 1 ~name ~(vec params)))

(defmacro check-third-call [ds name & params]
  `(check-call ~ds 2 ~name ~(vec params)))

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
  [gewgaw :unpacker true])

(defentity Keyed
  [other-key :type (foreign-key :other)])

(defentity :address
  [line-1]
  [postal-code]
  [state :default "Illinois"])

(defentity :person
  [first-name]
  [last-name]
  [address :type :address])

(defentity :address
  [line-1]
  [postal-code]
  [state :default "Illinois"])

(defentity :person
  [first-name]
  [last-name]
  [address :type :address])

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
    (set-ds! nil)
    (should-throw Exception "No Datastore bound (hyperion/*ds*). Use clojure.core/binding to bind a value or hyperion.api/set-ds! to globally set it."
      (ds)))

  (it "the ds can be bound"
    (let [fake-ds (new-fake-datastore)]
      (binding [*ds* fake-ds]
        (should= fake-ds (ds)))))

  (it "the ds can be installed"
    (let [fake-ds (new-fake-datastore)]
      (try
        (set-ds! fake-ds)
        (should= fake-ds (ds))
        (finally (set-ds! nil)))))

  (it "knows if a record is new"
    (should= true (new? {:kind "new"}))
    (should= false (new? {:kind "old" :key "exists"})))

  (it "translates filters"
    (should= := (#'hyperion.api/->filter-operator := ))
    (should= := (#'hyperion.api/->filter-operator "="))
    (should= := (#'hyperion.api/->filter-operator "eq"))
    (should= :!= (#'hyperion.api/->filter-operator :!= ))
    (should= :!= (#'hyperion.api/->filter-operator "not"))
    (should= :> (#'hyperion.api/->filter-operator :> ))
    (should= :> (#'hyperion.api/->filter-operator "gt"))
    (should= :< (#'hyperion.api/->filter-operator :< ))
    (should= :< (#'hyperion.api/->filter-operator "lt"))
    (should= :>= (#'hyperion.api/->filter-operator :>= ))
    (should= :>= (#'hyperion.api/->filter-operator "gte"))
    (should= :<= (#'hyperion.api/->filter-operator :<= ))
    (should= :<= (#'hyperion.api/->filter-operator "lte"))
    (should= :contains? (#'hyperion.api/->filter-operator :contains? ))
    (should= :contains? (#'hyperion.api/->filter-operator "contains?"))
    (should= :contains? (#'hyperion.api/->filter-operator "in"))
    (should-throw Exception "Unknown filter operator: foo" (#'hyperion.api/->filter-operator "foo")))

  (it "translates sort directions"
    (should= :asc (#'hyperion.api/->sort-direction :asc ))
    (should= :asc (#'hyperion.api/->sort-direction :ascending ))
    (should= :asc (#'hyperion.api/->sort-direction "asc"))
    (should= :asc (#'hyperion.api/->sort-direction "ascending"))
    (should= :desc (#'hyperion.api/->sort-direction :desc ))
    (should= :desc (#'hyperion.api/->sort-direction :descending ))
    (should= :desc (#'hyperion.api/->sort-direction "desc"))
    (should= :desc (#'hyperion.api/->sort-direction "descending"))
    (should-throw Exception "Unknown sort direction: foo" (#'hyperion.api/->sort-direction "foo")))

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
        (check-first-call (ds) "ds-save" [{:kind "packable" :widget 42 :gewgaw nil :bauble ""}]))

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
          (check-first-call (ds) "ds-save" [{:kind "packable" :widget 42 :bauble "" :gewgaw nil}]))

        (it "nested types"
          (save (person :first-name "Myles"))
          (check-first-call (ds) "ds-save" [{:kind "person" :first-name "Myles" :last-name nil :address {:kind "address" :line-1 nil :postal-code nil :state "Illinois"}}]))

        (it "nested types merges given nested data"
          (save (person :first-name "Myles" :address {:line-1 "Home"}))
          (check-first-call (ds) "ds-save" [{:kind "person" :first-name "Myles" :last-name nil :address {:kind "address" :line-1 "Home" :postal-code nil :state "Illinois"}}]))

        (it "custom functions"
          (save {:kind :packable :bauble "hello"})
          (check-first-call (ds) "ds-save" [{:kind "packable" :widget nil :bauble "olleh" :gewgaw nil}]))

        (it "keys which are ds-specific"
          (reset! (.responses (ds)) ["packed-abc123"])
          (save {:kind :keyed :other-key "abc123"})
          (check-first-call (ds) "ds-pack-key" "abc123")
          (check-second-call (ds) "ds-save" [{:kind "keyed" :other-key "packed-abc123"}]))

        (it "nil key"
          (save {:kind :keyed :other-key nil})
          (check-first-call (ds) "ds-save" [{:kind "keyed" :other-key nil}]))

        (it "applies default values and packs them"
          (save {:kind :many-defaulted-fields})
          (check-first-call (ds) "ds-save" [{:kind "many-defaulted-fields"
                                             :field1 nil
                                             :field2 nil
                                             :field3 ".141592"
                                             :field42 "24eulav"}]))

               )

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
            (should= {:kind "packable" :bauble "VAL" :widget nil :gewgaw nil} (save {})))

          (it "kind for known kind"
            (reset! (.responses (ds)) [[{:kind :packable}]])
            (should= {:kind "packable" :bauble nil :widget nil :gewgaw nil} (save {}))))

        (it "types"
          (reset! (.responses (ds)) [[{:kind "packable" :widget 42}]])
          (should= {:kind "packable" :bauble nil :widget "42" :gewgaw nil} (save {})))

        (it "nested types does not apply defaults"
          (reset! (.responses (ds)) [[{:kind "person" :first-name "Myles"}]])
          (should= {:kind "person" :first-name "Myles" :last-name nil :address {:kind "address" :line-1 nil :postal-code nil :state nil}} (save {})))

        (it "nested types merges given nested data"
          (reset! (.responses (ds)) [[{:kind "person" :first-name "Myles" :address {:line-1 "Home"}}]])
          (should= {:kind "person" :first-name "Myles" :last-name nil :address {:kind "address" :line-1 "Home" :postal-code nil :state nil}} (save {})))

        (it "unknown kinds"
          (reset! (.responses (ds)) [[{:kind "unknown" :widget 42}]])
          (should= {:kind "unknown" :widget 42} (save {})))

        (it "custom functions"
          (reset! (.responses (ds)) [[{:kind "packable" :bauble "hello"}]])
          (should= {:kind "packable" :bauble "HELLO" :widget nil :gewgaw nil} (save {})))

        (it "does not apply default values"
          (reset! (.responses (ds)) [[{:kind "many-defaulted-fields"}]])
          (should= {:kind "many-defaulted-fields" :field1 nil :field2 nil :field3 nil :field42 nil} (save {})))

        (it "nil"
          (reset! (.responses (ds)) [[nil]])
          (should= nil (save {})))

        (it "keys which are ds-specific"
          (reset! (.responses (ds)) [[{:kind "keyed" :other-key "abc123"}] "unpacked-abc123"])
          (should= {:kind "keyed" :other-key "unpacked-abc123"} (save {}))
          (check-first-call (ds) "ds-save" [{}])
          (check-second-call (ds) "ds-unpack-key" "other" "abc123"))

        (it "nil key"
          (reset! (.responses (ds)) [[{:kind "keyed" :other-key nil}] "unpacked-abc123"])
          (should= {:kind "keyed" :other-key nil} (save {}))
          (check-first-call (ds) "ds-save" [{}])
          (check-second-call (ds) nil))
        )

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
                                                 :updated-at mock-date}]))))
        )
      )
    )

  (context "factory"

    (it "bombs on unkown implementation"
      (should-throw Exception "Can't find datastore implementation: nonexistent"
        (new-datastore :implementation "nonexistent")))

    (it "bombs on missing implementation"
      (should-throw Exception "new-datastore requires an :implementation entry (:memory, :mysql, :mongo, ...)"
        (new-datastore)))

    (it "manufactures a memory database"
      (let [ds (new-datastore :implementation :memory)]
        (should= "hyperion.memory.MemoryDatastore" (.getName (class ds)))))

    )
  )
