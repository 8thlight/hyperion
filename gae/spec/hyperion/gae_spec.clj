(ns hyperion.gae-spec
  (:use
    [speclj.core]
    [hyperion.core]
    [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
    [hyperion.gae.spec-helper :only (with-local-datastore)]
    [hyperion.gae :only (new-gae-datastore) :as gae]
    [clojure.string :only (upper-case)])
  (:import
    [com.google.appengine.api.datastore Key ShortBlob Blob Category Email GeoPt Link IMHandle PostalAddress Rating PhoneNumber Text]
    [com.google.appengine.api.users User]
    [com.google.appengine.api.blobstore BlobKey]))

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

(defentity VarietyShow
  [short-blob :type ShortBlob]
  [blob :type Blob]
  [category :type Category]
  [email :type Email]
  [geo-pt :type GeoPt]
  [user :type User]
  [blob-key :type BlobKey]
  [link :type Link]
  [imhandle :type IMHandle]
  [address :type PostalAddress]
  [rating :type Rating]
  [phone :type PhoneNumber]
  [text :type Text]
  [other :type Key])

(defentity CustomPacking
  [bauble :packer #(apply str (reverse %)) :unpacker upper-case])

(defentity Hooks
  [field]
  [create-message]
  [save-message]
  [load-message])

(defmethod after-create :hooks [record]
  (assoc record :create-message (str "created with: " (:field record))))

(defmethod before-save :hooks [record]
  (assoc record :save-message (str "saving with: " (:field record))))

(defmethod after-load :hooks [record]
  (assoc record :load-message (str "loaded with: " (:field record))))

(defentity Timestamps
  [created-at]
  [updated-at])

(describe "Google AppEngine Datastore"

  (with-local-datastore)
  (around [it]
    (binding [*ds* (new-gae-datastore)]
      (it)))

  (it-behaves-like-a-datastore)

 ; (it "saves and loads a hollow entity"
 ;   (let [unsaved (hollow)
 ;         saved (save unsaved)]
 ;     (should-not= nil (:key saved))
 ;     (let [loaded (find-by-key (:key saved))]
 ;       (should= (:key saved) (:key loaded))
 ;       (should-not-be-same saved loaded))))

 ; (it "can update existing enitity"
 ;   (let [saved (save (one-field :field "giyup"))
 ;         updated (save saved :field "kia")]
 ;     (should= (:key saved) (:key updated))
 ;     (should= "kia" (:field updated))
 ;     (should= "kia" (:field (find-by-key (:key saved))))))

 ; (it "can save multiple entities at once"
 ;   (let [result (save* [(hollow) (one-field :field 1)])]
 ;     (should= 2 (count result))
 ;     (should= "hollow" (:kind (find-by-key (:key (first result)))))
 ;     (should= "one-field" (:kind (find-by-key (:key (second result)))))))

 ; (it "can delete an entity"
 ;   (let [saved (save (one-field :field "hi"))]
 ;     (delete saved)
 ;     (should= nil (find-by-key (:key saved)))))

 ; (it "only saves the fields defined in the entity"
 ;   (let [unsaved (one-field :foo "foo")
 ;         saved (save unsaved)
 ;         loaded (find-by-key (:key saved))
 ;         raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;     (should= "foo" (:foo unsaved))
 ;     (should= nil (:foo saved))
 ;     (should= nil (:foo loaded))
 ;     (should= nil (.getProperty raw "foo"))
 ;     (should= true (.containsKey (.getProperties raw) "field"))
 ;     (should= true (contains? loaded :field))))

 ; (it "can find-by multiple keys at same time"
 ;   (let [one (save (one-field :field 1))
 ;         two (save (one-field :field 2))
 ;         three (save (one-field :field 3))]
 ;     (should= [one two three] (find-by-keys [(:key one) (:key two) (:key three)]))
 ;     (should= [three two one] (find-by-keys [(:key three) (:key two) (:key one)]))
 ;     (should= [one nil three] (find-by-keys [(:key one) (gae/create-key "foo" 1) (:key three)]))))

 ; (context "Keys"

 ;   (it "can create a key"
 ;     (should= "one-field" (.getKind (gae/create-key "one-field" 42)))
 ;     (should= 42 (.getId (gae/create-key "one-field" 42)))
 ;     (should= "foo" (.getName (gae/create-key "one-field" "foo"))))

 ;   (it "knows a key when it sees one"
 ;     (should= false (gae/key? "foo"))
 ;     (should= false (gae/key? 42))
 ;     (should= true (gae/key? (gae/create-key "one-field" 42))))

 ;   (it "converts a key to a string and back"
 ;     (let [key (gae/create-key "one-field" 42)
 ;           str-value (gae/key->string key)]
 ;       (should= String (class str-value))
 ;       (should= key (gae/string->key str-value))))

 ;   (it "returns nil when converting bad keys"
 ;     (should= nil (gae/string->key "blah"))
 ;     (should= nil (gae/string->key :fooey))
 ;     (should= nil (gae/key->string nil)))

 ;   )

 ; (context "searching"

 ;   (it "finds by kind"
 ;     (should= [] (find-by-kind "hollow"))
 ;     (let [saved (save (hollow))]
 ;       (should= [saved] (find-by-kind "hollow"))
 ;       (let [saved2 (save (hollow))]
 ;         (should= [saved saved2] (find-by-kind "hollow")))))

 ;   (it "finds by kind with multiple kinds"
 ;     (let [saved-hollow (save (hollow))
 ;           saved-one (save (one-field))
 ;           saved-many (save (many-fields))]
 ;       (should= [saved-hollow] (find-by-kind "hollow"))
 ;       (should= [saved-one] (find-by-kind 'one-field))
 ;       (should= [saved-many] (find-by-kind :many-fields))))

 ;   (it "handles filters to find-by-kind"
 ;     (let [one (save (one-field :field 1))
 ;           five (save (one-field :field 5))
 ;           ten (save (one-field :field 10))]
 ;       (should= [one five ten] (find-by-kind :one-field))
 ;       (should= [one] (find-by-kind :one-field :filters [:= :field 1]))
 ;       (should= [five] (find-by-kind :one-field :filters [:= :field 5]))
 ;       (should= [one five] (find-by-kind :one-field :filters [:<= :field 5]))
 ;       (should= [ten] (find-by-kind :one-field :filters [:> :field 5]))
 ;       (should= [five ten] (find-by-kind :one-field :filters [:>= :field 5]))
 ;       (should= [one ten] (find-by-kind :one-field :filters [:!= :field 5]))
 ;       (should= [one ten] (find-by-kind :one-field :filters [:not :field 5]))
 ;       (should= [five] (find-by-kind :one-field :filters [:contains? :field [4 5 6]]))
 ;       (should= [five] (find-by-kind :one-field :filters [:in :field [4 5 6]]))
 ;       (should= [five] (find-by-kind :one-field :filters [[:> :field 1] [:< :field 10]]))
 ;       (should= [] (find-by-kind :one-field :filters [[:> :field 1] [:< :field 10] [:not :field 5]]))))

 ;   (it "handles sort order to find-by-kind"
 ;     (let [three (save (many-fields :field1 3 :field2 "odd"))
 ;           one (save (many-fields :field1 1 :field2 "odd"))
 ;           four (save (many-fields :field1 4 :field2 "even"))
 ;           five (save (many-fields :field1 5 :field2 "odd"))
 ;           nine (save (many-fields :field1 9 :field2 "odd"))
 ;           two (save (many-fields :field1 2 :field2 "even"))]
 ;       (should= [one two three four five nine] (find-by-kind "many-fields" :sorts [:field1 :asc]))
 ;       (should= [nine five four three two one] (find-by-kind "many-fields" :sorts [:field1 :desc]))
 ;       (should= [three one five nine four two] (find-by-kind "many-fields" :sorts [:field2 "desc"]))
 ;       (should= [four two three one five nine] (find-by-kind "many-fields" :sorts [:field2 "asc"]))
 ;       (should= [two four one three five nine] (find-by-kind "many-fields" :sorts [[:field2 "asc"] [:field1 :asc]]))))

 ;   (it "handles fetch options"
 ;     (let [three (save (many-fields :field1 3 :field2 "odd"))
 ;           one (save (many-fields :field1 1 :field2 "odd"))
 ;           four (save (many-fields :field1 4 :field2 "even"))
 ;           five (save (many-fields :field1 5 :field2 "odd"))
 ;           nine (save (many-fields :field1 9 :field2 "odd"))
 ;           two (save (many-fields :field1 2 :field2 "even"))]
 ;       (should= [one two] (find-by-kind "many-fields" :sorts [:field1 :asc] :limit 2 :offset 0))
 ;       (should= [three four] (find-by-kind "many-fields" :sorts [:field1 :asc] :limit 2 :offset 2))
 ;       (should= [five nine] (find-by-kind "many-fields" :sorts [:field1 :asc] :limit 2 :offset 4))))

 ;   (it "can count by kind"
 ;     (let [three (save (many-fields :field1 3 :field2 "odd"))
 ;           one (save (many-fields :field1 1 :field2 "odd"))
 ;           four (save (many-fields :field1 4 :field2 "even"))
 ;           five (save (many-fields :field1 5 :field2 "odd"))
 ;           nine (save (many-fields :field1 9 :field2 "odd"))
 ;           two (save (many-fields :field1 2 :field2 "even"))]
 ;       (should= 6 (count-by-kind "many-fields"))
 ;       (should= 4 (count-by-kind "many-fields" :filters [:< :field1 5]))
 ;       (should= 1 (count-by-kind "many-fields" :filters [:> :field1 5]))
 ;       (should= 4 (count-by-kind "many-fields" :filters [:= :field2 "odd"]))
 ;       (should= 2 (count-by-kind "many-fields" :filters [:= :field2 "even"]))))

 ;   (it "finds all kinds"
 ;     (let [h1 (save (hollow))
 ;           o1 (save (one-field :field 1))
 ;           m1 (save (many-fields :field1 2))]
 ;       (should= (set (map :key [h1 o1 m1])) (set (map :key (find-all-kinds))))
 ;       (should= [h1] (find-all-kinds :filters [[:< :__key__ (gae/create-key "hollow" 100)]]))
 ;       (should= [h1] (find-all-kinds :limit 1))))

 ;   (it "counts all kinds"
 ;     (let [h1 (save (hollow))
 ;           o1 (save (one-field :field 1))
 ;           m1 (save (many-fields :field1 2))]
 ;       (should= 3 (count-all-kinds))
 ;       (should= 1 (count-all-kinds :filters [[:< :__key__ (gae/create-key "hollow" 100)]]))))


 ;   (context "handles data types:"
 ;     (it "ShortBlob"
 ;       (let [saved (save (variety-show :short-blob (.getBytes "Short" "UTF-8")))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= ShortBlob (class (.getProperty raw "short-blob")))
 ;         (should= "Short" (String. (.getBytes (.getProperty raw "short-blob"))))
 ;         (should= "Short" (String. (:short-blob (find-by-key (:key saved)))))))

 ;     (it "Blob"
 ;       (let [saved (save (variety-show :blob (.getBytes "Blob" "UTF-8")))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= Blob (class (.getProperty raw "blob")))
 ;         (should= "Blob" (String. (.getBytes (.getProperty raw "blob"))))
 ;         (should= "Blob" (String. (:blob (find-by-key (:key saved)))))))

 ;     (it "Category"
 ;       (let [saved (save (variety-show :category "red"))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= Category (class (.getProperty raw "category")))
 ;         (should= "red" (.getCategory (.getProperty raw "category")))
 ;         (should= "red" (:category (find-by-key (:key saved))))))

 ;     (it "Email"
 ;       (let [saved (save (variety-show :email "joe@blow.com"))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= Email (class (.getProperty raw "email")))
 ;         (should= "joe@blow.com" (.getEmail (.getProperty raw "email")))
 ;         (should= "joe@blow.com" (:email (find-by-key (:key saved))))))

 ;     (it "GeoPt"
 ;       (let [saved (save (variety-show :geo-pt {:latitude 12.34 :longitude 56.78}))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))
 ;             loaded (find-by-key (:key saved))]
 ;         (should= GeoPt (class (.getProperty raw "geo-pt")))
 ;         (should= 12.34 (.getLatitude (.getProperty raw "geo-pt")) 0.01)
 ;         (should= 56.78 (.getLongitude (.getProperty raw "geo-pt")) 0.01)
 ;         (should= 12.34 (:latitude (:geo-pt loaded)) 0.01)
 ;         (should= 56.78 (:longitude (:geo-pt loaded)) 0.01)))

 ;     (it "User"
 ;       (let [saved (save (variety-show :user {:email "joe@blow.com" :auth-domain "gmail.com" :user-id "1234567890"}))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))
 ;             loaded (find-by-key (:key saved))]
 ;         (should= User (class (.getProperty raw "user")))
 ;         (should= "joe@blow.com" (.getEmail (.getProperty raw "user")))
 ;         (should= "joe@blow.com" (:email (:user loaded)))))

 ;     (it "BlobKey"
 ;       (let [saved (save (variety-show :blob-key "4321"))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= BlobKey (class (.getProperty raw "blob-key")))
 ;         (should= "4321" (.getKeyString (.getProperty raw "blob-key")))
 ;         (should= "4321" (:blob-key (find-by-key (:key saved))))))

 ;     (it "Link"
 ;       (let [saved (save (variety-show :link "http://gaeshi.org"))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= Link (class (.getProperty raw "link")))
 ;         (should= "http://gaeshi.org" (.getValue (.getProperty raw "link")))
 ;         (should= "http://gaeshi.org" (:link (find-by-key (:key saved))))))

 ;     (it "IMHandle"
 ;       (let [saved (save (variety-show :imhandle {:protocol "sip" :address "somewhere"}))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= IMHandle (class (.getProperty raw "imhandle")))
 ;         (should= "somewhere" (.getAddress (.getProperty raw "imhandle")))
 ;         (should= {:protocol "sip" :address "somewhere"} (:imhandle (find-by-key (:key saved))))))

 ;     (it "PostalAddress"
 ;       (let [saved (save (variety-show :address "123 Elm"))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= PostalAddress (class (.getProperty raw "address")))
 ;         (should= "123 Elm" (.getAddress (.getProperty raw "address")))
 ;         (should= "123 Elm" (:address (find-by-key (:key saved))))))

 ;     (it "Rating"
 ;       (let [saved (save (variety-show :rating 42))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= Rating (class (.getProperty raw "rating")))
 ;         (should= 42 (.getRating (.getProperty raw "rating")))
 ;         (should= 42 (:rating (find-by-key (:key saved))))))

 ;     (it "PhoneNumber"
 ;       (let [saved (save (variety-show :phone "555-867-5309"))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= PhoneNumber (class (.getProperty raw "phone")))
 ;         (should= "555-867-5309" (.getNumber (.getProperty raw "phone")))
 ;         (should= "555-867-5309" (:phone (find-by-key (:key saved))))))

 ;     (it "Text"
 ;       (let [saved (save (variety-show :text "some text"))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= Text (class (.getProperty raw "text")))
 ;         (should= "some text" (.getValue (.getProperty raw "text")))
 ;         (should= "some text" (:text (find-by-key (:key saved))))))

 ;     (it "Key"
 ;       (let [other (save (hollow))
 ;             saved (save (variety-show :other (:key other)))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= Key (class (.getProperty raw "other")))
 ;         (should= (:key other) (gae/key->string (.getProperty raw "other")))
 ;         (should= (:key other) (:other (find-by-key (:key saved))))))

 ;     (it "many Keys"
 ;       (let [other1 (save (hollow))
 ;             other2 (save (hollow))
 ;             saved (save (variety-show :other [(:key other1) (:key other2)]))
 ;             raw (.get (.service (ds)) (gae/->key (:key saved)))]
 ;         (should= [Key Key] (map class (.getProperty raw "other")))
 ;         (should= [(:key other1) (:key other2)] (map gae/key->string (.getProperty raw "other")))
 ;         (should= [(:key other1) (:key other2)] (:other (find-by-key (:key saved))))))

 ;     (context "in queries"

 ;       (it "for keys"
 ;         (let [other (save (hollow))
 ;               saved (save (variety-show :other (:key other)))
 ;               result (find-by-kind "variety-show" :filters [:= :other (:key other)])]
 ;           (should= 1 (count result))
 ;           (should= saved (first result))))

 ;       (it "for contains keys"
 ;         (let [other (save (hollow))
 ;               saved (save (variety-show :other (:key other)))
 ;               result (find-by-kind "variety-show" :filters [:in :other [(:key other)]])]
 ;           (should= 1 (count result))
 ;           (should= saved (first result))))

 ;       (it "for GeoPt"
 ;         (let [saved (save (variety-show :geo-pt {:latitude 12.34 :longitude 56.78}))
 ;               result (find-by-kind "variety-show" :filters [:eq :geo-pt {:latitude 12.34 :longitude 56.78}])]
 ;           (should= 1 (count result))
 ;           (should= saved (first result)))
 ;         )
 ;       )
 ;     )


 ;   (it "can store multiple values in one field"
 ;     (let [unsaved (one-field :field [1 2 3 4 5])
 ;           saved (save unsaved)
 ;           raw (.get (.service (ds)) (gae/->key (:key saved)))
 ;           loaded (find-by-key (:key saved))]
 ;       (should= [1 2 3 4 5] (:field loaded))
 ;       (should= java.util.ArrayList (class (.getProperty raw "field")))))

 ;   )
  )

(run-specs)
