(ns hyperion.gae.types
  (:use [hyperion.core :only [pack unpack]])
  (:import [com.google.appengine.api.datastore
            Key KeyFactory ShortBlob Blob Category Email GeoPt Link
            IMHandle IMHandle$Scheme PostalAddress Rating PhoneNumber Text]
           [com.google.appengine.api.users User]
           [com.google.appengine.api.blobstore BlobKey]
           [java.net URL]))

(defn map->user [values]
  (cond
    (nil? values) nil
    (contains? values :federated-identity ) (User. (:email values) (:auth-domain values) (:user-id values) (:federated-identity values))
    (contains? values :user-id ) (User. (:email values) (:auth-domain values) (:user-id values))
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

(defmethod unpack Key [_ value]
  (when value
    (KeyFactory/keyToString value)))

(defmethod pack ShortBlob [_ value]
  (cond
    (nil? value) nil
    (= ShortBlob (class value)) value
    :else (ShortBlob. value)))

(defmethod unpack ShortBlob [_ value]
  (when value
    (.getBytes value)))

(defmethod pack Blob [_ value]
  (cond
    (nil? value) nil
    (= Blob (class value)) value
    :else (Blob. value)))

(defmethod unpack Blob [_ value]
  (when value
    (.getBytes value)))

(defmethod pack Category [_ value]
  (cond
    (nil? value) nil
    (= Category (class value)) value
    :else (Category. value)))

(defmethod unpack Category [_ value]
  (when value
    (.getCategory value)))

(defmethod pack Email [_ value]
  (cond
    (nil? value) nil
    (= Email (class value)) value
    :else (Email. value)))

(defmethod unpack Email [_ value]
  (when value
    (.getEmail value)))

(defmethod pack GeoPt [_ value]
  (cond
    (nil? value) nil
    (= GeoPt (class value)) value
    :else (GeoPt. (:latitude value) (:longitude value))))

(defmethod unpack GeoPt [_ value]
  (when value
    {:latitude (.getLatitude value) :longitude (.getLongitude value)}))

(defmethod pack User [_ value]
  (cond
    (nil? value) nil
    (= User (class value)) value
    :else (map->user value)))

(defmethod unpack User [_ value]
  (user->map value))

(defmethod pack BlobKey [_ value]
  (cond
    (nil? value) nil
    (= BlobKey (class value)) value
    :else (BlobKey. value)))

(defmethod unpack BlobKey [_ value]
  (when value
    (.getKeyString value)))

(defmethod pack Link [_ value]
  (cond
    (nil? value) nil
    (= Link (class value)) value
    :else (Link. value)))

(defmethod unpack Link [_ value]
  (when value
    (.getValue value)))

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

(defmethod unpack IMHandle [_ value]
  (when value
    {:protocol (.getProtocol value) :address (.getAddress value)}))

(defmethod pack PostalAddress [_ value]
  (cond
    (nil? value) nil
    (= PostalAddress (class value)) value
    :else (PostalAddress. value)))

(defmethod unpack PostalAddress [_ value]
  (when value
    (.getAddress value)))

(defmethod pack Rating [_ value]
  (cond
    (nil? value) nil
    (= Rating (class value)) value
    :else (Rating. value)))

(defmethod unpack Rating [_ value]
  (when value
    (.getRating value)))

(defmethod pack PhoneNumber [_ value]
  (cond
    (nil? value) nil
    (= PhoneNumber (class value)) value
    :else (PhoneNumber. value)))

(defmethod unpack PhoneNumber [_ value]
  (when value
    (.getNumber value)))

(defmethod pack Text [_ value]
  (cond
    (nil? value) nil
    (= Text (class value)) value
    :else (Text. value)))

(defmethod unpack Text [_ value]
  (when value
    (.getValue value)))





