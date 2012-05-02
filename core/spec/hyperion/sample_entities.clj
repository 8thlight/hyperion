(ns hyperion.sample-entities
  (use
    [hyperion.core]
    [clojure.string :only (upper-case)]))

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

;(defentity VarietyShow
;  [short-blob :type ShortBlob]
;  [blob :type Blob]
;  [category :type Category]
;  [email :type Email]
;  [geo-pt :type GeoPt]
;  [user :type User]
;  [blob-key :type BlobKey]
;  [link :type Link]
;  [imhandle :type IMHandle]
;  [address :type PostalAddress]
;  [rating :type Rating]
;  [phone :type PhoneNumber]
;  [text :type Text]
;  [other :type Key])

(defentity CustomPacking
  [bauble :packer #(apply str (reverse %)) :unpacker upper-case])

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