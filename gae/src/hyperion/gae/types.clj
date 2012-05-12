(ns hyperion.gae.types
  (:use
    [hyperion.core :only [pack Packed]])
  (:import
    [com.google.appengine.api.datastore
     Key KeyFactory ShortBlob Blob Category Email GeoPt Link
     IMHandle IMHandle$Scheme PostalAddress Rating PhoneNumber Text]
    [com.google.appengine.api.users User]
    [com.google.appengine.api.blobstore BlobKey]
    [java.net URL]))

(defn map->user [values]
  (cond
    (contains? values :federated-identity) (User. (:email values) (:auth-domain values) (:user-id values) (:federated-identity values))
    (contains? values :user-id) (User. (:email values) (:auth-domain values) (:user-id values))
    :else (User. (:email values) (:auth-domain values))))

(defn user->map [user]
  (if user
    {:email (.getEmail user)
     :nickname (.getNickname user)
     :auth-domain (.getAuthDomain user)
     :user-id (.getUserId user)
     :federated-identity (.getFederatedIdentity user)}
    nil))

(defmethod pack Key [_ value]
  (cond
    (nil? value) nil
    (= Key (class value)) value
    :else (KeyFactory/stringToKey value)))

(defmethod pack ShortBlob [_ value]
  (cond
    (nil? value) nil
    (= ShortBlob (class value)) value
    :else (ShortBlob. value)))

(defmethod pack Blob [_ value]
  (cond
    (nil? value) nil
    (= Blob (class value)) value
    :else (Blob. value)))

(defmethod pack Category [_ value]
  (cond
    (nil? value) nil
    (= Category (class value)) value
    :else (Category. value)))

(defmethod pack Email [_ value]
  (cond
    (nil? value) nil
    (= Email (class value)) value
    :else (Email. value)))

(defmethod pack GeoPt [_ value]
  (cond
    (nil? value) nil
    (= GeoPt (class value)) value
    :else (GeoPt. (:latitude value) (:longitude value))))

(defmethod pack User [_ value]
  (cond
    (nil? value) nil
    (= User (class value)) value
    :else (map->user value)))

(defmethod pack BlobKey [_ value]
  (cond
    (nil? value) nil
    (= BlobKey (class value)) value
    :else (BlobKey. value)))

(defmethod pack Link [_ value]
  (cond
    (nil? value) nil
    (= Link (class value)) value
    :else (Link. value)))

(defn parse-im-protocol [protocol]
  (try
    (IMHandle$Scheme/valueOf (name protocol))
    (catch Exception e
      (try
        (URL. protocol)
        (catch Exception e
          IMHandle$Scheme/unknown)))))

(defmethod pack IMHandle [_ value]
  (cond
    (nil? value) nil
    (= IMHandle (class value)) value
    :else (IMHandle. (parse-im-protocol (:protocol value)) (:address value))))

(defmethod pack PostalAddress [_ value]
  (cond
    (nil? value) nil
    (= PostalAddress (class value)) value
    :else (PostalAddress. value)))

(defmethod pack Rating [_ value]
  (cond
    (nil? value) nil
    (= Rating (class value)) value
    :else (Rating. value)))

(defmethod pack PhoneNumber [_ value]
  (cond
    (nil? value) nil
    (= PhoneNumber (class value)) value
    :else (PhoneNumber. value)))

(defmethod pack Text [_ value]
  (cond
    (nil? value) nil
    (= Text (class value)) value
    :else (Text. value)))

(extend-type nil
  Packed
  (unpack [this] nil))

(extend-type Key
  Packed
  (unpack [this] (KeyFactory/keyToString this)))

(extend-type ShortBlob
  Packed
  (unpack [this] (.getBytes this)))

(extend-type Blob
  Packed
  (unpack [this] (.getBytes this)))

(extend-type Category
  Packed
  (unpack [this] (.getCategory this)))

(extend-type Email
  Packed
  (unpack [this] (.getEmail this)))

(extend-type GeoPt
  Packed
  (unpack [this] {:latitude (.getLatitude this) :longitude (.getLongitude this)}))

(extend-type User
  Packed
  (unpack [this] (user->map this)))

(extend-type BlobKey
  Packed
  (unpack [this] (.getKeyString this)))

(extend-type Link
  Packed
  (unpack [this] (.getValue this)))

(extend-type IMHandle
  Packed
  (unpack [this] {:protocol (.getProtocol this) :address (.getAddress this)}))

(extend-type PostalAddress
  Packed
  (unpack [this] (.getAddress this)))

(extend-type Rating
  Packed
  (unpack [this] (.getRating this)))

(extend-type PhoneNumber
  Packed
  (unpack [this] (.getNumber this)))

(extend-type Text
  Packed
  (unpack [this] (.getValue this)))





