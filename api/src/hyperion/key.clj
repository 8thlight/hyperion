(ns hyperion.key
  (:require [clojure.data.codec.base64 :refer [encode decode]]
            [hyperion.abstr :refer :all]))

(defn encode-key [value]
  (.replace
    (String. (encode (.getBytes value)))
    "="
    ""))

(defn decode-key [value]
  (String.
    (decode
      (.getBytes
        (case (mod (.length value) 4)
          3 (str value "=")
          2 (str value "==")
          1 (str value "===")
          value)))))

(defn generate-id []
  (.replace (str (java.util.UUID/randomUUID)) "-" ""))

(defn compose-key
  ([kind] (compose-key kind (generate-id)))
  ([kind id] (encode-key (str (encode-key (->kind kind)) ":" (encode-key (str id))))))

(defn decompose-key [key]
  (map decode-key (.split (decode-key key) ":")))

