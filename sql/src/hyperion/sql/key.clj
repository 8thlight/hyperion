(ns hyperion.sql.key
  (:require [clojure.data.codec.base64 :refer [encode decode]]
            [clojure.string :as string]))

(defn create-key [^String kind id]
  (when (or (nil? id) (not (integer? id)))
    (throw (Exception. (str "id must be integer to create key. was: " id))))
  (String. (encode (.getBytes (str kind ":" id)))))

(defn decompose-key [^String key]
  (let [[kind id-str] (string/split (String. (decode (.getBytes key))) #":")]
    (list kind (Long/parseLong id-str))))
