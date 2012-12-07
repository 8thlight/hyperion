(ns hyperion.api-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer :all]
            [hyperion.log :as log]
            [hyperion.filtering :refer [make-filter]]
            [hyperion.sorting :refer [make-sort]]
            [hyperion.fake :refer :all]
            [hyperion.types :refer [foreign-key]]
            [clojure.string :refer (upper-case)]
            [chee.datetime :refer [now before? seconds-ago between? seconds-from-now]]))

(log/error!)

(defmacro check-call [ds index name params]
  `(let [call# (get @(.calls ~ds) ~index)]
     (should= ~name (first call#))
     (should= (seq ~params) (second call#))))

(defmacro did-not-call [ds name]
  `(should-not-contain ~name (map first @(.calls ~ds))))

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

    (it "packs fields"
      (call-fn "packable" :filters [:= :something-key "1"])
      (assert-fn "packable" [(make-filter := :something-id "1")]))

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

(defentity :many-defaulted-fields [field1]
  [field2]
  [field3 :default ".141592" :packer identity]
  [field4 :default 12345]
  [field42 :default "value42" :packer #(apply str (reverse %))])

(defentity "PACKABLE"
  [something-key :db-name :something-id ]
  [widget :type Integer]
  [bauble :packer #(apply str (reverse %)) :unpacker #(if % (upper-case %) %)]
  [gewgaw :unpacker true])

(defentity Keyed
  [other-key :type (foreign-key :other )])

(defentity Thing
  [thing]
  [other-thing])

(defmethod pack Integer [_ value]
  (when value
    (Integer. (Integer/parseInt value))))

(defmethod unpack Integer [_ value]
  (when value
    (str value)))

(defentity Hooks
  [field]
  [create-message]
  [save-message]
  [load-message])

(defn save-empty []
  (save {:kind "test"}))

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

      (it "throws an exception when saving a record without a kind"
        (let [to-save {:value 42}]
          (should-throw
            Exception
            (str "Cannot save without specifying a kind: " to-save)
            (save to-save))))

      (it "saves records with values as options"
        (save {:kind "one"} :value 42)
        (check-first-call (ds) "ds-save" [{:kind "one" :value 42}]))

      (it "saves a value that is true on a field that has the same name as the kind"
        (save {:kind "value"} :value true)
        (check-first-call (ds) "ds-save" [{:kind "value" :value true}]))

      (it "saves a value that is false on a field that has a different name than the kind"
        (save {:kind "thing"} :other-thing false)
        (check-first-call (ds) "ds-save" [{:kind "thing" :other-thing false :thing nil}]))

      (it "saves a value that is false on a field that has the same name as the kind"
        (save {:kind "thing"} :thing false)
        (check-first-call (ds) "ds-save" [{:kind "thing" :thing false :other-thing nil}]))

      (it "saves records with values as a map"
        (save {:kind "one"} {:field "kia" :value 42})
        (check-first-call (ds) "ds-save" [{:kind "one" :value 42 :field "kia"}]))

      (it "saves multiple entities at once"
        (save* {:kind "one" :value 42} {:kind "two" :value 35})
        (check-first-call (ds) "ds-save" [{:kind "one" :value 42} {:kind "two" :value 35}]))

      (it "packs entities"
        (save {:kind :packable :widget "42"})
        (check-first-call (ds) "ds-save" [{:kind "packable"
                                           :widget 42
                                           :gewgaw nil
                                           :bauble ""
                                           :something-id nil}]))

      (it "converts the kind to a string"
        (save {:kind :one})
        (check-first-call (ds) "ds-save" [{:kind "one"}])))

    (context "delete by key"
      (it "delete by key"
        (delete-by-key "one42")
        (check-first-call (ds) "ds-delete-by-key" "one42"))

      (it "returns nil for a empty string key"
        (should-be-nil (delete-by-key ""))
        (did-not-call (ds) "ds-delete-by-key"))

      (it "finds the key in the ds"
        (delete-by-key "one42")
        (check-first-call (ds) "ds-delete-by-key" "one42"))

      )

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
      (reset! (.responses (ds)) [{:thing 1 :kind "none"}])
      (should= {:thing 1 :kind "none"} (reload {:kind "kind" :key 1}))
      (check-first-call (ds) "ds-find-by-key" 1))

    (context "find by key"
      (it "returns nil for a nil key"
        (should-be-nil (find-by-key nil))
        (did-not-call (ds) "ds-find-by-key"))

      (it "returns nil for a empty string key"
        (should-be-nil (find-by-key ""))
        (did-not-call (ds) "ds-find-by-key"))

      (it "finds the key in the ds"
        (find-by-key "one42")
        (check-first-call (ds) "ds-find-by-key" "one42"))

      )

    (context "find by kind"
      (it "finds with only a kind"
        (let [response [{:thing 1 :kind "none"}]]
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

      (defentity OneField
        [field])

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
          (check-first-call (ds) "ds-save" [{:kind "packable"
                                             :widget 42
                                             :bauble ""
                                             :gewgaw nil
                                             :something-id nil}]))

        (it "custom functions"
          (save {:kind :packable :bauble "hello"})
          (check-first-call (ds) "ds-save" [{:kind "packable"
                                             :widget nil
                                             :bauble "olleh"
                                             :gewgaw nil
                                             :something-id nil}]))

        (it "keys which are ds-specific"
          (reset! (.responses (ds)) ["packed-abc123"])
          (save {:kind :keyed :other-key "abc123"})
          (check-first-call (ds) "ds-pack-key" "abc123")
          (check-second-call (ds) "ds-save" [{:kind "keyed"
                                              :other-key "packed-abc123"}]))

        (it "nil key"
          (save {:kind :keyed :other-key nil})
          (check-first-call (ds) "ds-save" [{:kind "keyed" :other-key nil}]))

        (it "applies default values and packs them"
          (save (many-defaulted-fields))
          (check-first-call (ds) "ds-save" [{:kind "many-defaulted-fields"
                                             :field1 nil
                                             :field2 nil
                                             :field3 ".141592"
                                             :field4 12345
                                             :field42 "24eulav"}]))

        (it "doesn't apply defaults when nil or false is set on a field"
          (save (many-defaulted-fields :field3 nil :field4 false))
          (check-first-call (ds) "ds-save" [{:kind "many-defaulted-fields"
                                             :field1 nil
                                             :field2 nil
                                             :field3 nil
                                             :field4 false
                                             :field42 "24eulav"}]))

        (defentity with-db-name
          [something-key :db-name :something-id :default 45]
          [other-key :db-name "other-id"])

        (it "packs db-name to field names"
          (save {:kind :with-db-name :something-key "12345"})
          (check-first-call (ds) "ds-save" [{:kind "with-db-name"
                                             :something-id "12345"
                                             :other-id nil}]))

        (it "packs db-name as string"
          (save (with-db-name :other-key "12345"))
          (check-first-call (ds) "ds-save" [{:kind "with-db-name"
                                             :something-id 45
                                             :other-id "12345"}]))

        (it "packs db-name with default"
          (save (with-db-name))
          (check-first-call (ds) "ds-save" [{:kind "with-db-name"
                                             :something-id 45
                                             :other-id nil}]))

        (it "doesn't normalize fields to spear case"
          (save {:kind :unknown :camelCasedFieldName "value"})
          (check-first-call (ds) "ds-save" [{:kind "unknown"
                                             :camelCasedFieldName "value"}]))

        (it "doesn't normalize fields to keywords"
          (save {:kind :unknown "camelCasedFieldName" "value"})
          (check-first-call (ds) "ds-save" [{:kind "unknown"
                                             "camelCasedFieldName" "value"}]))

        )

      (context "unpacking"

        (it "normalizes kind for unknown kind"
          (reset! (.responses (ds)) [[{:kind :UNknown}]])
          (should= {:kind "unknown"} (save-empty)))

        (it "normalizes attribues to keyword for unknown kind"
          (reset! (.responses (ds)) [[{:kind "unknown" "some_WEIRD_Field" :val}]])
          (should= {:kind "unknown" :some_WEIRD_Field :val} (save-empty)))

        (it "normalizes kind for known kind"
          (reset! (.responses (ds)) [[{:kind :packable}]])
          (should= "packable" (:kind (save-empty))))

        (it "normalizes attribues for known kind"
          (reset! (.responses (ds)) [[{:kind "packable" :BAuble "val"}]])
          (should= {:kind "packable" :bauble nil :widget nil :gewgaw nil :something-key nil} (save-empty)))

        (it "types"
          (reset! (.responses (ds)) [[{:kind "packable" :widget 42}]])
          (should= {:kind "packable" :bauble nil :widget "42" :gewgaw nil :something-key nil} (save-empty)))

        (defentity with-other-db-name
          [something-key :db-name :something-id ])

        (it "unpacks db-name"
          (reset! (.responses (ds)) [[{:kind "with-other-db-name" :something-id 42}]])
          (should= {:kind "with-other-db-name" :something-key 42} (save-empty)))

        (it "unknown kinds"
          (reset! (.responses (ds)) [[{:kind "unknown" :widget 42}]])
          (should= {:kind "unknown" :widget 42} (save-empty)))

        (it "custom functions"
          (reset! (.responses (ds)) [[{:kind "packable" :bauble "hello"}]])
          (should= {:kind "packable" :bauble "HELLO" :widget nil :gewgaw nil :something-key nil} (save-empty)))

        (it "does not apply default values"
          (reset! (.responses (ds)) [[{:kind "many-defaulted-fields"}]])
          (should= {:kind "many-defaulted-fields" :field1 nil :field2 nil :field3 nil :field4 nil :field42 nil} (save-empty)))

        (it "nil"
          (reset! (.responses (ds)) [[nil]])
          (should= nil (save-empty)))

        (it "keys which are ds-specific"
          (reset! (.responses (ds)) [[{:kind "keyed" :other-key "abc123"}] "unpacked-abc123"])
          (should= {:kind "keyed" :other-key "unpacked-abc123"} (save-empty))
          (check-first-call (ds) "ds-save" [{:kind "test"}])
          (check-second-call (ds) "ds-unpack-key" "other" "abc123"))

        (it "nil key"
          (reset! (.responses (ds)) [[{:kind "keyed" :other-key nil}] "unpacked-abc123"])
          (should= {:kind "keyed" :other-key nil} (save-empty))
          (check-first-call (ds) "ds-save" [{:kind "test"}])
          (check-second-call (ds) nil))
        )

      (context "hooks"
        (context "after create"
          (it "known kind"
            (let [unsaved (hooks :field "waza!")]
              (should= "waza!" (:field unsaved))
              (should= "created with: waza!" (:create-message unsaved))))

          (it "on unknown kind never runs"
            (defmethod after-create :unknown-kind-after-create [record]
              (assoc record :my-cool-field :value ))
            (reset! (.responses (ds)) [[{:kind "unknown-kind-after-create"}]])
            (should= {:kind "unknown-kind-after-create"} (save-empty)))
          )

        (context "after load"

          (it "on unknown kind is called"
            (defmethod after-load :unknown-kind-after-load [record]
              (assoc record :my-cool-field :value ))
            (reset! (.responses (ds)) [[{:kind "unknown-kind-after-load"}]])
            (should= {:kind "unknown-kind-after-load" :my-cool-field :value} (save-empty)))
          )

        (it "has before save hook"
          (save (hooks :field "waza!"))
          (check-first-call (ds) "ds-save" [{:kind "hooks"
                                             :field "waza!"
                                             :create-message "created with: waza!"
                                             :save-message "saving with: waza!"
                                             :load-message nil}]))

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

      (it "can have extra fields when created"
        (let [unsaved-thing (thing :extra "goodies")
              saved-thing (save unsaved-thing)]
          (should= "goodies" (:extra unsaved-thing))
          (should= nil (:extra saved-thing))))
      )
    )

  (context "factory"

    (it "bombs on unknown implementation"
      (should-throw Exception "Can't find datastore implementation: nonexistent"
        (new-datastore :implementation "nonexistent")))

    (it "bombs on missing implementation"
      (should-throw Exception "new-datastore requires an :implementation entry (:memory, :mysql, :mongo, ...)"
        (new-datastore)))

    (it "manufactures a memory database"
      (let [ds (new-datastore :implementation :memory )]
        (should= "hyperion.memory.MemoryDatastore" (.getName (class ds)))))

    )
  )

