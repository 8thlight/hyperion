(ns hyperion.core-spec
  (:use
    [speclj.core]
    [hyperion.core]
    [hyperion.fake]
    [hyperion.sample-entities]))

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

    (context "and entities"

      (it "defines simple entities"
        (let [instance (hollow)]
          (should= hyperion.sample_entities.Hollow (class instance)))))

    (it "caches the entity definition"
      (let [entities @(deref #'*entities*)
            hollow-entity (get entities "hollow")]
        (should= 1 (count hollow-entity))
        (should= true (fn? (:*ctor* hollow-entity)))))

    (it "defines entity with one field"
      (let [instance (one-field :field "value")]
        (should= hyperion.sample_entities.OneField (class instance))
        (should= "value" (:field instance))))

    (it "caches the one-field definition"
      (let [entities @(deref #'*entities*)
            entity-def (get entities "one-field")]
        (should= 2 (count entity-def))
        (should= true (fn? (:*ctor* entity-def)))
        (should= {} (:field entity-def))))

    (it "defines an entity with multiple field"
      (let [instance (many-fields :field1 "value" :field2 "value2" :field42 "value42")]
        (should= hyperion.sample_entities.ManyFields (class instance))
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

    (it "saves and loads a hollow entity"
      (let [unsaved (hollow)
            saved (save unsaved)]
        (should-not= nil (:key saved))
        (let [loaded (find-by-key (:key saved))]
          (should= (:key saved) (:key loaded))
          (should-not-be-same saved loaded))))
    ;    ;
    ;      (it "can set values while saving"
    ;        (let [unsaved (one-field)
    ;              saved (save unsaved :field "kia")]
    ;          (should= "kia" (:field saved))
    ;          (should= "kia" (:field (find-by-key (:key saved))))))
    ;
    ;      (it "can set values using a map while saving"
    ;        (let [unsaved (one-field)
    ;              saved (save unsaved {:field "Hiya!"})]
    ;          (should= "Hiya!" (:field saved))
    ;          (should= "Hiya!" (:field (find-by-key (:key saved))))))
    ;
    ;      (it "can update existing enitity"
    ;        (let [saved (save (one-field :field "giyup"))
    ;              updated (save saved :field "kia")]
    ;          (should= (:key saved) (:key updated))
    ;          (should= "kia" (:field updated))
    ;          (should= "kia" (:field (find-by-key (:key saved))))))
    ;
    ;      (it "can save multiple entities at once"
    ;        (let [result (save-many [(hollow) (one-field :field 1)])]
    ;          (should= 2 (count result))
    ;          (should= "hollow" (:kind (find-by-key (:key (first result)))))
    ;          (should= "one-field" (:kind (find-by-key (:key (second result)))))))
    ;
    ;      (it "can delete an entity"
    ;        (let [saved (save (one-field :field "hi"))]
    ;          (delete saved)
    ;          (should= nil (find-by-key (:key saved)))))
    ;
    ;      (it "only saves the fields defined in the entity"
    ;        (let [unsaved (one-field :foo "foo")
    ;              saved (save unsaved)
    ;              loaded (find-by-key (:key saved))
    ;              raw (.get (datastore-service) (->key (:key saved)))]
    ;          (should= "foo" (:foo unsaved))
    ;          (should= nil (:foo saved))
    ;          (should= nil (:foo loaded))
    ;          (should= nil (.getProperty raw "foo"))
    ;          (should= true (.containsKey (.getProperties raw) "field"))
    ;          (should= true (contains? loaded :field))))
    ;
    ;      (it "can find-by multiple keys at same time"
    ;        (let [one (save (one-field :field 1))
    ;              two (save (one-field :field 2))
    ;              three (save (one-field :field 3))]
    ;          (should= [one two three] (find-by-keys [(:key one) (:key two) (:key three)]))
    ;          (should= [three two one] (find-by-keys [(:key three) (:key two) (:key one)]))
    ;          (should= [one nil three] (find-by-keys [(:key one) (create-key "foo" 1) (:key three)]))))
    ;
    ;      (it "find-by-key(s) doesn't raise error when passed nil-ish values"
    ;        (let [one (save (one-field :field 1))]
    ;          (should-not-throw (find-by-key nil))
    ;          (should= nil (find-by-key nil))
    ;          (should= nil (find-by-key ""))
    ;          (should= nil (find-by-key "bad key"))
    ;          (should= [] (find-by-keys [nil]))
    ;          (should= [one] (find-by-keys [nil (:key one)]))
    ;          (should= [] (find-by-keys nil))))
    ;
    ;      (it "can find-by-key using stringified keys"
    ;        (let [one (save (one-field :field 1))
    ;              two (save (one-field :field 2))
    ;              three (save (one-field :field 3))]
    ;          (should= one (find-by-key (:key one)))
    ;          (should= two (find-by-key (:key two)))
    ;          (should= [one two three] (find-by-keys (map :key [one two three])))))
    ;
    ;      (it "converts to key"
    ;        (let [one (save (one-field :field 1))
    ;              akey (string->key (:key one))]
    ;          (should= akey (->key akey))
    ;          (should= akey (->key (key->string akey)))
    ;          (should= akey (->key one))
    ;          (should= nil (->key nil))))
    ;
    ;      (it "reloads entities"
    ;        (let [unsaved (one-field :field 1)
    ;              saved (save unsaved)]
    ;          (should= nil (reload unsaved))
    ;          (should= nil (reload (create-key "blah" 123)))
    ;          (should= 1 (:field (reload saved)))
    ;          (save saved :field 2)
    ;          (should= 2 (:field (reload saved)))
    ;          (should= 2 (:field (reload (:key saved))))))
    ;
    ;      (context "Keys"
    ;
    ;        (it "can create a key"
    ;          (should= "one-field" (.getKind (create-key "one-field" 42)))
    ;          (should= 42 (.getId (create-key "one-field" 42)))
    ;          (should= "foo" (.getName (create-key "one-field" "foo"))))
    ;
    ;        (it "knows a key when it sees one"
    ;          (should= false (key? "foo"))
    ;          (should= false (key? 42))
    ;          (should= true (key? (create-key "one-field" 42))))
    ;
    ;        (it "converts a key to a string and back"
    ;          (let [key (create-key "one-field" 42)
    ;                str-value (key->string key)]
    ;            (should= String (class str-value))
    ;            (should= key (string->key str-value))))
    ;
    ;        (it "returns nil when converting bad keys"
    ;          (should= nil (string->key "blah"))
    ;          (should= nil (string->key :fooey))
    ;          (should= nil (key->string nil)))
    ;
    ;        )
    ;
    ;      (context "searching"
    ;
    ;        (it "finds by kind"
    ;          (should= [] (find-by-kind "hollow"))
    ;          (let [saved (save (hollow))]
    ;            (should= [saved] (find-by-kind "hollow"))
    ;            (let [saved2 (save (hollow))]
    ;              (should= [saved saved2] (find-by-kind "hollow")))))
    ;
    ;        (it "finds by kind with multiple kinds"
    ;          (let [saved-hollow (save (hollow))
    ;                saved-one (save (one-field))
    ;                saved-many (save (many-fields))]
    ;            (should= [saved-hollow] (find-by-kind "hollow"))
    ;            (should= [saved-one] (find-by-kind 'one-field))
    ;            (should= [saved-many] (find-by-kind :many-fields))))
    ;
    ;        (it "handles filters to find-by-kind"
    ;          (let [one (save (one-field :field 1))
    ;                five (save (one-field :field 5))
    ;                ten (save (one-field :field 10))]
    ;            (should= [one five ten] (find-by-kind :one-field))
    ;            (should= [one] (find-by-kind :one-field :filters [:= :field 1]))
    ;            (should= [five] (find-by-kind :one-field :filters [:= :field 5]))
    ;            (should= [one five] (find-by-kind :one-field :filters [:<= :field 5]))
    ;            (should= [ten] (find-by-kind :one-field :filters [:> :field 5]))
    ;            (should= [five ten] (find-by-kind :one-field :filters [:>= :field 5]))
    ;            (should= [one ten] (find-by-kind :one-field :filters [:!= :field 5]))
    ;            (should= [one ten] (find-by-kind :one-field :filters [:not :field 5]))
    ;            (should= [five] (find-by-kind :one-field :filters [:contains? :field [4 5 6]]))
    ;            (should= [five] (find-by-kind :one-field :filters [:in :field [4 5 6]]))
    ;            (should= [five] (find-by-kind :one-field :filters [[:> :field 1] [:< :field 10]]))
    ;            (should= [] (find-by-kind :one-field :filters [[:> :field 1] [:< :field 10] [:not :field 5]]))))
    ;
    ;        (it "handles sort order to find-by-kind"
    ;          (let [three (save (many-fields :field1 3 :field2 "odd"))
    ;                one (save (many-fields :field1 1 :field2 "odd"))
    ;                four (save (many-fields :field1 4 :field2 "even"))
    ;                five (save (many-fields :field1 5 :field2 "odd"))
    ;                nine (save (many-fields :field1 9 :field2 "odd"))
    ;                two (save (many-fields :field1 2 :field2 "even"))]
    ;            (should= [one two three four five nine] (find-by-kind "many-fields" :sorts [:field1 :asc]))
    ;            (should= [nine five four three two one] (find-by-kind "many-fields" :sorts [:field1 :desc]))
    ;            (should= [three one five nine four two] (find-by-kind "many-fields" :sorts [:field2 "desc"]))
    ;            (should= [four two three one five nine] (find-by-kind "many-fields" :sorts [:field2 "asc"]))
    ;            (should= [two four one three five nine] (find-by-kind "many-fields" :sorts [[:field2 "asc"] [:field1 :asc]]))))
    ;
    ;        (it "handles fetch options"
    ;          (let [three (save (many-fields :field1 3 :field2 "odd"))
    ;                one (save (many-fields :field1 1 :field2 "odd"))
    ;                four (save (many-fields :field1 4 :field2 "even"))
    ;                five (save (many-fields :field1 5 :field2 "odd"))
    ;                nine (save (many-fields :field1 9 :field2 "odd"))
    ;                two (save (many-fields :field1 2 :field2 "even"))]
    ;            (should= [one two] (find-by-kind "many-fields" :sorts [:field1 :asc] :limit 2 :offset 0))
    ;            (should= [three four] (find-by-kind "many-fields" :sorts [:field1 :asc] :limit 2 :offset 2))
    ;            (should= [five nine] (find-by-kind "many-fields" :sorts [:field1 :asc] :limit 2 :offset 4))))
    ;
    ;        (it "can count by kind"
    ;          (let [three (save (many-fields :field1 3 :field2 "odd"))
    ;                one (save (many-fields :field1 1 :field2 "odd"))
    ;                four (save (many-fields :field1 4 :field2 "even"))
    ;                five (save (many-fields :field1 5 :field2 "odd"))
    ;                nine (save (many-fields :field1 9 :field2 "odd"))
    ;                two (save (many-fields :field1 2 :field2 "even"))]
    ;            (should= 6 (count-by-kind "many-fields"))
    ;            (should= 4 (count-by-kind "many-fields" :filters [:< :field1 5]))
    ;            (should= 1 (count-by-kind "many-fields" :filters [:> :field1 5]))
    ;            (should= 4 (count-by-kind "many-fields" :filters [:= :field2 "odd"]))
    ;            (should= 2 (count-by-kind "many-fields" :filters [:= :field2 "even"]))
    ;            (should= 2 (count-by-kind "many-fields" :limit 2))
    ;            (should= 5 (count-by-kind "many-fields" :limit 5))
    ;            (should= 4 (count-by-kind "many-fields" :offset 2))
    ;            (should= 1 (count-by-kind "many-fields" :offset 5))
    ;            (should= 2 (count-by-kind "many-fields" :offset 2 :limit 2))))
    ;
    ;        (it "finds all kinds"
    ;          (let [h1 (save (hollow))
    ;                o1 (save (one-field :field 1))
    ;                m1 (save (many-fields :field1 2))]
    ;            (should= (set (map :key [h1 o1 m1])) (set (map :key (find-all-kinds))))
    ;            (should= [h1] (find-all-kinds :filters [[:< :__key__ (create-key "hollow" 100)]]))
    ;            (should= [h1] (find-all-kinds :limit 1))))
    ;
    ;        (it "counts all kinds"
    ;          (let [h1 (save (hollow))
    ;                o1 (save (one-field :field 1))
    ;                m1 (save (many-fields :field1 2))]
    ;            (should= 3 (count-all-kinds))
    ;            (should= 1 (count-all-kinds :filters [[:< :__key__ (create-key "hollow" 100)]]))
    ;            (should= 1 (count-all-kinds :limit 1))))
    ;        )
    ;
    ;      (context "handles data types:"
    ;        (it "ShortBlob"
    ;          (let [saved (save (variety-show :short-blob (.getBytes "Short" "UTF-8")))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= ShortBlob (class (.getProperty raw "short-blob")))
    ;            (should= "Short" (String. (.getBytes (.getProperty raw "short-blob"))))
    ;            (should= "Short" (String. (:short-blob (find-by-key (:key saved)))))))
    ;
    ;        (it "Blob"
    ;          (let [saved (save (variety-show :blob (.getBytes "Blob" "UTF-8")))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= Blob (class (.getProperty raw "blob")))
    ;            (should= "Blob" (String. (.getBytes (.getProperty raw "blob"))))
    ;            (should= "Blob" (String. (:blob (find-by-key (:key saved)))))))
    ;
    ;        (it "Category"
    ;          (let [saved (save (variety-show :category "red"))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= Category (class (.getProperty raw "category")))
    ;            (should= "red" (.getCategory (.getProperty raw "category")))
    ;            (should= "red" (:category (find-by-key (:key saved))))))
    ;
    ;        (it "Email"
    ;          (let [saved (save (variety-show :email "joe@blow.com"))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= Email (class (.getProperty raw "email")))
    ;            (should= "joe@blow.com" (.getEmail (.getProperty raw "email")))
    ;            (should= "joe@blow.com" (:email (find-by-key (:key saved))))))
    ;
    ;        (it "GeoPt"
    ;          (let [saved (save (variety-show :geo-pt {:latitude 12.34 :longitude 56.78}))
    ;                raw (.get (datastore-service) (->key (:key saved)))
    ;                loaded (find-by-key (:key saved))]
    ;            (should= GeoPt (class (.getProperty raw "geo-pt")))
    ;            (should= 12.34 (.getLatitude (.getProperty raw "geo-pt")) 0.01)
    ;            (should= 56.78 (.getLongitude (.getProperty raw "geo-pt")) 0.01)
    ;            (should= 12.34 (:latitude (:geo-pt loaded)) 0.01)
    ;            (should= 56.78 (:longitude (:geo-pt loaded)) 0.01)))
    ;
    ;        (it "User"
    ;          (let [saved (save (variety-show :user {:email "joe@blow.com" :auth-domain "gmail.com" :user-id "1234567890"}))
    ;                raw (.get (datastore-service) (->key (:key saved)))
    ;                loaded (find-by-key (:key saved))]
    ;            (should= User (class (.getProperty raw "user")))
    ;            (should= "joe@blow.com" (.getEmail (.getProperty raw "user")))
    ;            (should= "joe@blow.com" (:email (:user loaded)))))
    ;
    ;        (it "BlobKey"
    ;          (let [saved (save (variety-show :blob-key "4321"))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= BlobKey (class (.getProperty raw "blob-key")))
    ;            (should= "4321" (.getKeyString (.getProperty raw "blob-key")))
    ;            (should= "4321" (:blob-key (find-by-key (:key saved))))))
    ;
    ;        (it "Link"
    ;          (let [saved (save (variety-show :link "http://gaeshi.org"))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= Link (class (.getProperty raw "link")))
    ;            (should= "http://gaeshi.org" (.getValue (.getProperty raw "link")))
    ;            (should= "http://gaeshi.org" (:link (find-by-key (:key saved))))))
    ;
    ;        (it "IMHandle"
    ;          (let [saved (save (variety-show :imhandle {:protocol "sip" :address "somewhere"}))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= IMHandle (class (.getProperty raw "imhandle")))
    ;            (should= "somewhere" (.getAddress (.getProperty raw "imhandle")))
    ;            (should= {:protocol "sip" :address "somewhere"} (:imhandle (find-by-key (:key saved))))))
    ;
    ;        (it "PostalAddress"
    ;          (let [saved (save (variety-show :address "123 Elm"))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= PostalAddress (class (.getProperty raw "address")))
    ;            (should= "123 Elm" (.getAddress (.getProperty raw "address")))
    ;            (should= "123 Elm" (:address (find-by-key (:key saved))))))
    ;
    ;        (it "Rating"
    ;          (let [saved (save (variety-show :rating 42))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= Rating (class (.getProperty raw "rating")))
    ;            (should= 42 (.getRating (.getProperty raw "rating")))
    ;            (should= 42 (:rating (find-by-key (:key saved))))))
    ;
    ;        (it "PhoneNumber"
    ;          (let [saved (save (variety-show :phone "555-867-5309"))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= PhoneNumber (class (.getProperty raw "phone")))
    ;            (should= "555-867-5309" (.getNumber (.getProperty raw "phone")))
    ;            (should= "555-867-5309" (:phone (find-by-key (:key saved))))))
    ;
    ;        (it "Text"
    ;          (let [saved (save (variety-show :text "some text"))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= Text (class (.getProperty raw "text")))
    ;            (should= "some text" (.getValue (.getProperty raw "text")))
    ;            (should= "some text" (:text (find-by-key (:key saved))))))
    ;
    ;        (it "Key"
    ;          (let [other (save (hollow))
    ;                saved (save (variety-show :other (:key other)))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= Key (class (.getProperty raw "other")))
    ;            (should= (:key other) (key->string (.getProperty raw "other")))
    ;            (should= (:key other) (:other (find-by-key (:key saved))))))
    ;
    ;        (it "many Keys"
    ;          (let [other1 (save (hollow))
    ;                other2 (save (hollow))
    ;                saved (save (variety-show :other [(:key other1) (:key other2)]))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= [Key Key] (map class (.getProperty raw "other")))
    ;            (should= [(:key other1) (:key other2)] (map key->string (.getProperty raw "other")))
    ;            (should= [(:key other1) (:key other2)] (:other (find-by-key (:key saved))))))
    ;
    ;        (context "in queries"
    ;
    ;          (it "for keys"
    ;            (let [other (save (hollow))
    ;                  saved (save (variety-show :other (:key other)))
    ;                  result (find-by-kind "variety-show" :filters [:= :other (:key other)])]
    ;              (should= 1 (count result))
    ;              (should= saved (first result))))
    ;
    ;          (it "for contains keys"
    ;            (let [other (save (hollow))
    ;                  saved (save (variety-show :other (:key other)))
    ;                  result (find-by-kind "variety-show" :filters [:in :other [(:key other)]])]
    ;              (should= 1 (count result))
    ;              (should= saved (first result))))
    ;
    ;          (it "for GeoPt"
    ;            (let [saved (save (variety-show :geo-pt {:latitude 12.34 :longitude 56.78}))
    ;                  result (find-by-kind "variety-show" :filters [:eq :geo-pt {:latitude 12.34 :longitude 56.78}])]
    ;              (should= 1 (count result))
    ;              (should= saved (first result)))
    ;            )
    ;          )
    ;        )
    ;
    ;      (it "allows custom packing"
    ;        (let [unsaved (custom-packing :bauble "hello")
    ;              saved (save unsaved)
    ;              raw (.get (datastore-service) (->key (:key saved)))
    ;              loaded (find-by-key (:key saved))]
    ;          (should= "hello" (:bauble unsaved))
    ;          (should= "olleh" (.getProperty raw "bauble"))
    ;          (should= "OLLEH" (:bauble loaded))))
    ;
    ;      (it "can store multiple values in one field"
    ;        (let [unsaved (one-field :field [1 2 3 4 5])
    ;              saved (save unsaved)
    ;              raw (.get (datastore-service) (->key (:key saved)))
    ;              loaded (find-by-key (:key saved))]
    ;          (should= [1 2 3 4 5] (:field loaded))
    ;          (should= java.util.ArrayList (class (.getProperty raw "field")))))
    ;
    ;      (context "Hooks"
    ;        (it "has after create hook"
    ;          (let [unsaved (hooks :field "waza!")]
    ;            (should= "waza!" (:field unsaved))
    ;            (should= "created with: waza!" (:create-message unsaved))))
    ;
    ;        (it "has before save hook"
    ;          (let [saved (save (hooks :field "waza!"))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (should= "saving with: waza!" (:save-message saved))
    ;            (should= "saving with: waza!" (.getProperty raw "save-message"))))
    ;
    ;        (it "has after load hook"
    ;          (let [unsaved (hooks :field "waza!")
    ;                saved (save unsaved)
    ;                raw (.get (datastore-service) (->key (:key saved)))
    ;                loaded (find-by-key (:key saved))]
    ;            (should= nil (:load-message unsaved))
    ;            (should= "loaded with: waza!" (:load-message saved))
    ;            (should= nil (.getProperty raw "load-message"))
    ;            (should= "loaded with: waza!" (:load-message loaded))))
    ;        )
    ;
    ;      (context "Timestamps"
    ;
    ;        (it "are automatically populated on save"
    ;          (let [saved (save (timestamps))]
    ;            (should-not= nil (:created-at saved))
    ;            (should-not= nil (:updated-at saved))
    ;            (should (before? (seconds-ago 1) (:created-at saved)))
    ;            (should (before? (seconds-ago 1) (:updated-at saved)))))
    ;
    ;        (it "are saved based on kind, not provided keys"
    ;          (let [saved (save {:kind "timestamps"})]
    ;            (should-not= nil (:created-at saved))
    ;            (should-not= nil (:updated-at saved))
    ;            (should (before? (seconds-ago 1) (:created-at saved)))
    ;            (should (before? (seconds-ago 1) (:updated-at saved)))))
    ;
    ;        (it "dont update existing created-at"
    ;          (let [saved (save (timestamps))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (.setProperty raw "created-at" (seconds-ago 5))
    ;            (.put (datastore-service) raw)
    ;            (let [resaved (save (find-by-key (:key saved)))
    ;                  loaded (find-by-key (:key resaved))]
    ;              (should= true (between? (:created-at loaded) (seconds-ago 6) (seconds-ago 4))))))
    ;
    ;        (it "does update existing updated-at"
    ;          (let [saved (save (timestamps))
    ;                raw (.get (datastore-service) (->key (:key saved)))]
    ;            (.setProperty raw "updated-at" (seconds-ago 5))
    ;            (.put (datastore-service) raw)
    ;            (let [resaved (save (find-by-key (:key saved)))
    ;                  loaded (find-by-key (:key resaved))]
    ;              (should= true (between? (:updated-at loaded) (seconds-ago 1) (seconds-from-now 1))))))
    ;        )

    )

  )

(run-specs)
