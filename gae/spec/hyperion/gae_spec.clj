(ns hyperion.gae-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer :all]
            [hyperion.log :as log]
            [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]
            [hyperion.gae.spec-helper :refer (with-local-datastore)]
            [hyperion.gae :refer (new-gae-datastore) :as gae]
            [clojure.string :refer (upper-case)])
  (:import [com.google.appengine.api.datastore Key ShortBlob Blob Category Email GeoPt Link IMHandle PostalAddress Rating PhoneNumber Text KeyFactory]
           [com.google.appengine.api.users User]
           [com.google.appengine.api.blobstore BlobKey]))

(log/error!)

(defentity Hollow)

(defentity OneField
  [field])

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

(defentity :types
  [bool]
  [bite :type java.lang.Byte]
  [inti :type java.lang.Integer]
  [lng]
  [flt :type java.lang.Float]
  [dbl])

(describe "Google AppEngine Datastore"

  (context "creation"

    (it "with no args"
      (let [ds (new-gae-datastore)]
        (should= com.google.appengine.api.datastore.DatastoreServiceImpl (class (.service ds)))))

    (it "with a service as an args"
      (let [service (com.google.appengine.api.datastore.DatastoreServiceFactory/getDatastoreService)
            ds (new-gae-datastore service)]
        (should= service (.service ds))))

    (it "with a service in the options"
      (let [service (com.google.appengine.api.datastore.DatastoreServiceFactory/getDatastoreService)
            ds (new-gae-datastore :service service)]
        (should= service (.service ds))))

    (it "using the core factory with no service"
      (let [ds (new-datastore :implementation :gae)]
        (should= com.google.appengine.api.datastore.DatastoreServiceImpl (class (.service ds)))))

    (it "using the core factory with service"
      (let [service (com.google.appengine.api.datastore.DatastoreServiceFactory/getDatastoreService)
            ds (new-datastore :implementation :gae :service service)]
        (should= service (.service ds))))

    )

  (context "live"

    (with-local-datastore)

    (it-behaves-like-a-datastore)

    (context "Keys"

      (it "converts a key to a string and back"
        (let [key (KeyFactory/createKey "-kind" (long 1))
              str-value (gae/unpack-key key)]
          (should= String (class str-value))
          (should= key (gae/pack-key str-value))))
      )

    (context "handles data types:"
      (it "ShortBlob"
        (let [saved (save (variety-show :short-blob (.getBytes "Short" "UTF-8")))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= ShortBlob (class (.getProperty raw "short-blob")))
          (should= "Short" (String. (.getBytes (.getProperty raw "short-blob"))))
          (should= "Short" (String. (:short-blob (find-by-key (:key saved)))))))

      (it "Blob"
        (let [saved (save (variety-show :blob (.getBytes "Blob" "UTF-8")))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= Blob (class (.getProperty raw "blob")))
          (should= "Blob" (String. (.getBytes (.getProperty raw "blob"))))
          (should= "Blob" (String. (:blob (find-by-key (:key saved)))))))

      (it "Category"
        (let [saved (save (variety-show :category "red"))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= Category (class (.getProperty raw "category")))
          (should= "red" (.getCategory (.getProperty raw "category")))
          (should= "red" (:category (find-by-key (:key saved))))))

      (it "Email"
        (let [saved (save (variety-show :email "joe@blow.com"))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= Email (class (.getProperty raw "email")))
          (should= "joe@blow.com" (.getEmail (.getProperty raw "email")))
          (should= "joe@blow.com" (:email (find-by-key (:key saved))))))

      (it "GeoPt"
        (let [saved (save (variety-show :geo-pt {:latitude 12.34 :longitude 56.78}))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))
              loaded (find-by-key (:key saved))]
          (should= GeoPt (class (.getProperty raw "geo-pt")))
          (should= 12.34 (.getLatitude (.getProperty raw "geo-pt")) 0.01)
          (should= 56.78 (.getLongitude (.getProperty raw "geo-pt")) 0.01)
          (should= 12.34 (:latitude (:geo-pt loaded)) 0.01)
          (should= 56.78 (:longitude (:geo-pt loaded)) 0.01)))

      (it "User"
        (let [saved (save (variety-show :user {:email "joe@blow.com" :auth-domain "gmail.com" :user-key "1234567890"}))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))
              loaded (find-by-key (:key saved))]
          (should= User (class (.getProperty raw "user")))
          (should= "joe@blow.com" (.getEmail (.getProperty raw "user")))
          (should= "joe@blow.com" (:email (:user loaded)))))

      (it "BlobKey"
        (let [saved (save (variety-show :blob-key "4321"))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= BlobKey (class (.getProperty raw "blob-key")))
          (should= "4321" (.getKeyString (.getProperty raw "blob-key")))
          (should= "4321" (:blob-key (find-by-key (:key saved))))))

      (it "Link"
        (let [saved (save (variety-show :link "http://gaeshi.org"))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= Link (class (.getProperty raw "link")))
          (should= "http://gaeshi.org" (.getValue (.getProperty raw "link")))
          (should= "http://gaeshi.org" (:link (find-by-key (:key saved))))))

      (it "IMHandle"
        (let [saved (save (variety-show :imhandle {:protocol "sip" :address "somewhere"}))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= IMHandle (class (.getProperty raw "imhandle")))
          (should= "somewhere" (.getAddress (.getProperty raw "imhandle")))
          (should= {:protocol "sip" :address "somewhere"} (:imhandle (find-by-key (:key saved))))))

      (it "PostalAddress"
        (let [saved (save (variety-show :address "123 Elm"))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= PostalAddress (class (.getProperty raw "address")))
          (should= "123 Elm" (.getAddress (.getProperty raw "address")))
          (should= "123 Elm" (:address (find-by-key (:key saved))))))

      (it "Rating"
        (let [saved (save (variety-show :rating 42))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= Rating (class (.getProperty raw "rating")))
          (should= 42 (.getRating (.getProperty raw "rating")))
          (should= 42 (:rating (find-by-key (:key saved))))))

      (it "PhoneNumber"
        (let [saved (save (variety-show :phone "555-867-5309"))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= PhoneNumber (class (.getProperty raw "phone")))
          (should= "555-867-5309" (.getNumber (.getProperty raw "phone")))
          (should= "555-867-5309" (:phone (find-by-key (:key saved))))))

      (it "Text"
        (let [saved (save (variety-show :text "some text"))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= Text (class (.getProperty raw "text")))
          (should= "some text" (.getValue (.getProperty raw "text")))
          (should= "some text" (:text (find-by-key (:key saved))))))

      (it "Key"
        (let [other (save (hollow))
              saved (save (variety-show :other (:key other)))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= Key (class (.getProperty raw "other")))
          (should= (:key other) (gae/unpack-key (.getProperty raw "other")))
          (should= (:key other) (:other (find-by-key (:key saved))))))

      (it "many Keys"
        (let [other1 (save (hollow))
              other2 (save (hollow))
              saved (save (variety-show :other [(:key other1) (:key other2)]))
              raw (.get (.service (ds)) (gae/pack-key (:key saved)))]
          (should= [Key Key] (map class (.getProperty raw "other")))
          (should= [(:key other1) (:key other2)] (map gae/unpack-key (.getProperty raw "other")))
          (should= [(:key other1) (:key other2)] (:other (find-by-key (:key saved))))))

      (context "in queries"

        (it "for keys"
          (let [other (save (hollow))
                saved (save (variety-show :other (:key other)))
                result (find-by-kind "variety-show" :filters [:= :other (:key other)])]
            (should= 1 (count result))
            (should= saved (first result))))

        (it "for contains keys"
          (let [other (save (hollow))
                saved (save (variety-show :other (:key other)))
                result (find-by-kind "variety-show" :filters [:in :other [(:key other)]])]
            (should= 1 (count result))
            (should= saved (first result))))

        (it "for GeoPt"
          (let [saved (save (variety-show :geo-pt {:latitude 12.34 :longitude 56.78}))
                result (find-by-kind "variety-show" :filters [:eq :geo-pt {:latitude 12.34 :longitude 56.78}])]
            (should= 1 (count result))
            (should= saved (first result)))
          )
        )
      )


    (it "can store multiple values in one field"
      (let [unsaved (one-field :field [1 2 3 4 5])
            saved (save unsaved)
            raw (.get (.service (ds)) (gae/pack-key (:key saved)))
            loaded (find-by-key (:key saved))]
        (should= [1 2 3 4 5] (:field loaded))
        (should= java.util.ArrayList (class (.getProperty raw "field")))))

    )
  )

(run-specs)
