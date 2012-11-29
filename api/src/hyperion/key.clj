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
  ([kind id] (encode-key (str (->kind kind) ":" (str id)))))

(defn decompose-key [key]
  (let [decoded (decode-key key)
        colon-index (.indexOf decoded (int \:))]
    (if (< colon-index 1)
      (throw (Exception. (str "Invalid key form: " decoded)))
      (list (.substring decoded 0 colon-index) (.substring decoded (inc colon-index))))))

