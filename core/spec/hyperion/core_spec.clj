(ns hyperion.core-spec
  (:use
    [speclj.core]
    [hyperion.core]
    [hyperion.fake]
    [hyperion.memory]
    [clojure.string :only (upper-case)]
    [chee.datetime :exclude [before after]]))

(defmacro check-call [ds name & params]
  `(let [call# (first @(.calls ~ds))]
    (swap! (.calls ~ds) rest)
    (should= ~name (first call#))
    (should= '~params (second call#))))

(defentity Hollow)

(defentity OneField
  [field])

(defentity ManyFields
  [field1]
  [field2]
  [field42])

(defentity ManyDefaultedFields
  [field1]
  [field2]
  [field3 :default ".141592"]
  [field42 :default "value42"])

(defentity CustomPacking
  [bauble :packer #(apply str (reverse %)) :unpacker upper-case]
  [thingy :unpacker true])

(defentity Hooks
  [field]
  [create-message]
  [save-message]
  [load-message])

(extend-type Hooks
  AfterCreate
  (after-create [this] (assoc this :create-message (str "created with: " (:field this))))
  BeforeSave
  (before-save [this] (assoc this :save-message (str "saving with: " (:field this))))
  AfterLoad
  (after-load [this] (assoc this :load-message (str "loaded with: " (:field this)))))

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

  (context "entity definition"

    (it "defines simple entities"
      (let [instance (hollow)]
        (should= Hollow (class instance))))

    (it "caches the entity definition"
      (let [entities @(deref #'*entity-specs*)
            hollow-entity (get entities "hollow")]
        (should= 1 (count hollow-entity))
        (should= true (fn? (:*ctor* hollow-entity)))))

    (it "defines entity with one field"
      (let [instance (one-field :field "value")]
        (should= OneField (class instance))
        (should= "value" (:field instance))))

    (it "caches the one-field definition"
      (let [entities @(deref #'*entity-specs*)
            entity-def (get entities "one-field")]
        (should= 2 (count entity-def))
        (should= true (fn? (:*ctor* entity-def)))
        (should= {} (:field entity-def))))

    (it "defines an entity with multiple field"
      (let [instance (many-fields :field1 "value" :field2 "value2" :field42 "value42")]
        (should= ManyFields (class instance))
        (should= "value" (:field1 instance))
        (should= "value2" (:field2 instance))
        (should= "value42" (:field42 instance))))

    (it "defines an entity with some fields with default values"
      (let [instance (many-defaulted-fields :field1 "value" :field2 "value2")]
        (should= "value" (:field1 instance))
        (should= "value2" (:field2 instance))
        (should= ".141592" (:field3 instance))
        (should= "value42" (:field42 instance))))

    (it "extra fields in constructor are included"
      (should= "foo" (:foo (many-fields :foo "foo")))
      (should= "foo" (:foo (many-defaulted-fields :foo "foo"))))
    )

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

    (it "handles simple find-by-kind"
      (find-by-kind "thing")
      (check-call @_ds "ds-find-by-kind" "thing" nil nil nil nil {})
      (find-by-kind :thing)
      (check-call @_ds "ds-find-by-kind" "thing" nil nil nil nil {})
      (find-by-kind 'thing)
      (check-call @_ds "ds-find-by-kind" "thing" nil nil nil nil {}))

    (it "translates filters"
      (find-by-kind "thing" :filters [:= :a :b])
      (check-call @_ds "ds-find-by-kind" "thing" [[:= :a :b]] nil nil nil {})
      (find-by-kind "thing" :filters [["=" :a :b] ["eq" :x :y]])
      (check-call @_ds "ds-find-by-kind" "thing" [[:= :a :b] [:= :x :y]] nil nil nil {}))

    (it "translates sorts"
      (find-by-kind "thing" :sorts [:a :asc])
      (check-call @_ds "ds-find-by-kind" "thing" nil [[:a :asc]] nil nil {})
      (find-by-kind "thing" :sorts [[:a "asc"] [:b :descending]])
      (check-call @_ds "ds-find-by-kind" "thing" nil [[:a :asc] [:b :desc]] nil nil {}))

    (it "pass along limit and offset"
      (find-by-kind "thing" :limit 5 :offset 6)
      (check-call @_ds "ds-find-by-kind" "thing" nil nil 5 6 {}))

    (it "can count-by-kind"
      (count-by-kind "thing" :filters ["eq" :a :b])
      (check-call @_ds "ds-count-by-kind" "thing" [[:= :a :b]] {})
      (count-by-kind :thing :filters ["eq" :a :b])
      (check-call @_ds "ds-count-by-kind" "thing" [[:= :a :b]] {}))

    (it "can find-all-kinds "
      (find-all-kinds :filters ["eq" :a :b] :sorts [:c "ascending"] :limit 32 :offset 43)
      (check-call @_ds "ds-find-all-kinds" [[:= :a :b]] [[:c :asc]] 32 43 {}))

    (it "can count-all-kinds "
      (count-all-kinds :filters ["eq" :a :b])
      (check-call @_ds "ds-count-all-kinds" [[:= :a :b]] {}))
    )

  (context "with memorystore"

    (with _ds (new-memory-datastore))
    (before (reset! DS @_ds))

    (it "saves records with values as options"
      (let [saved (save {:kind "one"} :value 42)]
        (should= 42 (:value saved))
        (should= 42 (:value (find-by-key (:key saved))))))

    (context "and entities"

      (it "saves and loads a hollow entity"
        (let [unsaved (hollow)
              saved (save unsaved)]
          (should-not= nil (:key saved))
          (let [loaded (find-by-key (:key saved))]
            (should= (:key saved) (:key loaded)))))

      (it "can set values while saving"
        (let [unsaved (one-field)
              saved (save unsaved :field "kia")]
          (should= "kia" (:field saved))
          (should= "kia" (:field (find-by-key (:key saved))))))

      (it "can set values using a map while saving"
        (let [unsaved (one-field)
              saved (save unsaved {:field "Hiya!"})]
          (should= "Hiya!" (:field saved))
          (should= "Hiya!" (:field (find-by-key (:key saved))))))

      (it "can update existing enitity"
        (let [saved (save (one-field :field "giyup"))
              updated (save saved :field "kia")]
          (should= (:key saved) (:key updated))
          (should= "kia" (:field updated))
          (should= "kia" (:field (find-by-key (:key saved))))))

      (it "can save multiple entities at once"
        (let [result (save* [(hollow) (one-field :field 1)])]
          (should= 2 (count result))
          (should= Hollow (class (first result)))
          (should= OneField (class (second result)))
          (should= "hollow" (:kind (find-by-key (:key (first result)))))
          (should= "one-field" (:kind (find-by-key (:key (second result)))))))

      (it "can delete an entity"
        (let [saved (save (one-field :field "hi"))]
          (delete saved)
          (should= nil (find-by-key (:key saved)))))

      (it "only saves the fields defined in the entity"
        (let [unsaved (one-field :foo "foo")
              saved (save unsaved)
              loaded (find-by-key (:key saved))
              raw (get @(.store (ds)) (->key (:key saved)))]
          (should= "foo" (:foo unsaved))
          (should= nil (:foo saved))
          (should= nil (:foo loaded))
          (should= nil (:foo raw))
          (should= false (contains? raw :foo))
          (should= true (contains? raw :field))
          (should= true (contains? loaded :field))))

      (it "can find-by multiple keys at same time"
        (let [one (save (one-field :field 1))
              two (save (one-field :field 2))
              three (save (one-field :field 3))]
          (should= [one two three] (find-by-keys [(:key one) (:key two) (:key three)]))
          (should= [three two one] (find-by-keys [(:key three) (:key two) (:key one)]))
          (should= [one nil three] (find-by-keys [(:key one) "blah" (:key three)]))))

      (it "find-by-key(s) doesn't raise error when passed nil-ish values"
        (let [one (save (one-field :field 1))]
          (should-not-throw (find-by-key nil))
          (should= nil (find-by-key nil))
          (should= nil (find-by-key ""))
          (should= nil (find-by-key "bad key"))
          (should= [] (find-by-keys [nil]))
          (should= [one] (find-by-keys [nil (:key one)]))
          (should= [] (find-by-keys nil))))

      (it "reloads entities"
        (let [unsaved (one-field :field 1)
              saved (save unsaved)]
          (should= nil (reload unsaved))
          (should= nil (reload "blah"))
          (should= 1 (:field (reload saved)))
          (save saved :field 2)
          (should= 2 (:field (reload saved)))
          (should= 2 (:field (reload (:key saved))))))

      (context "searching"

        (it "finds by kind"
          (should= [] (find-by-kind "hollow"))
          (let [saved (save (hollow))]
            (should= [saved] (find-by-kind "hollow"))
            (let [saved2 (save (hollow))]
              (should= #{saved saved2} (set (find-by-kind "hollow"))))))

        )

      (it "allows custom packing"
        (let [unsaved (custom-packing :bauble "hello")
              saved (save unsaved)
              raw (get @(.store (ds)) (->key (:key saved)))
              loaded (find-by-key (:key saved))]
          (should= "hello" (:bauble unsaved))
          (should= "olleh" (:bauble raw))
          (should= "OLLEH" (:bauble loaded))))

      (it "can unpack nil"
        (let [saved (save (custom-packing :thingy nil))
              loaded (find-by-key (:key saved))]
          (should= nil (:thingy saved))
          (should= nil (:thingy loaded))))

      (it "can unpack strings"
        (let [saved (save (custom-packing :thingy "something"))
              loaded (find-by-key (:key saved))]
          (should= "something" (:thingy saved))
          (should= "something" (:thingy loaded))))

      (context "Hooks"
        (it "has after create hook"
          (let [unsaved (hooks :field "waza!")]
            (should= "waza!" (:field unsaved))
            (should= "created with: waza!" (:create-message unsaved))))

        (it "has before save hook"
          (let [saved (save (hooks :field "waza!"))
                raw (get @(.store (ds)) (->key (:key saved)))]
            (should= "saving with: waza!" (:save-message saved))
            (should= "saving with: waza!" (:save-message raw))))

        (it "has after load hook"
          (let [unsaved (hooks :field "waza!")
                saved (save unsaved)
                raw (get @(.store (ds)) (->key (:key saved)))
                loaded (find-by-key (:key saved))]
            (should= nil (:load-message unsaved))
            (should= "loaded with: waza!" (:load-message saved))
            (should= nil (:load-message raw))
            (should= "loaded with: waza!" (:load-message loaded))))
        )

      (context "Timestamps"

        (it "are automatically populated on save"
          (let [saved (save (timestamps))]
            (should-not= nil (:created-at saved))
            (should-not= nil (:updated-at saved))
            (should (before? (seconds-ago 1) (:created-at saved)))
            (should (before? (seconds-ago 1) (:updated-at saved)))))

        (it "are automatically populated on save*"
          (let [saved (first (save* [(timestamps)]))]
            (should-not= nil (:created-at saved))
            (should-not= nil (:updated-at saved))
            (should (before? (seconds-ago 1) (:created-at saved)))
            (should (before? (seconds-ago 1) (:updated-at saved)))))

        (it "are saved based on kind, not provided keys"
          (let [saved (save {:kind "timestamps"})]
            (should-not= nil (:created-at saved))
            (should-not= nil (:updated-at saved))
            (should (before? (seconds-ago 1) (:created-at saved)))
            (should (before? (seconds-ago 1) (:updated-at saved)))))

        (it "dont update existing created-at"
          (let [saved (save (timestamps))
                key (->key (:key saved))
                raw (get @(.store (ds)) key)
                raw (assoc raw :created-at (seconds-ago 5))]
            (dosync
              (alter (.store (ds)) assoc key raw))
            (let [resaved (save (find-by-key (:key saved)))
                  loaded (find-by-key (:key resaved))]
              (should= true (between? (:created-at loaded) (seconds-ago 6) (seconds-ago 4))))))

        (it "does update existing updated-at"
          (let [saved (save (timestamps))
                key (->key (:key saved))
                raw (get @(.store (ds)) key)
                raw (assoc raw :updated-at (seconds-ago 5))]
            (dosync
              (alter (.store (ds)) assoc key raw))
            (let [resaved (save (find-by-key (:key saved)))
                  loaded (find-by-key (:key resaved))]
              (should= true (between? (:updated-at loaded) (seconds-ago 1) (seconds-from-now 1))))))
        )
      )
    )
  )

(run-specs)
