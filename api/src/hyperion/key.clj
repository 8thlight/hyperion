(ns hyperion.key
  (:require [clojure.data.codec.base64 :refer [encode decode]]
            [hyperion.abstr :refer :all])
  (:import [java.lang IllegalArgumentException]))

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

(def key-fodder (.toCharArray "ABCDEFGHIJKLMNOPabcdefghijklmnop0123456789"))
(def fodder-count (count key-fodder))

(defn random-fodder-seq
  ([] (random-fodder-seq (java.util.Random. (System/nanoTime))))
  ([generator]
    (cons
      (aget key-fodder (.nextInt generator fodder-count))
      (lazy-seq (random-fodder-seq generator)))))

(defn generate-id []
  (let [buffer (StringBuffer.)]
    (doseq [c (take 8 (random-fodder-seq))]
      (.append buffer c))
    (.toString buffer)))

;(defn generate-id []
;  (.replace (str (java.util.UUID/randomUUID)) "-" ""))

(defn compose-key
  ([kind] (compose-key kind (generate-id)))
  ([kind id] (encode-key (str (->kind kind) ":" (str id)))))

(defn decompose-key [key]
  (let [decoded (decode-key key)
        colon-index (.indexOf decoded (int \:))]
    (if (< colon-index 1)
      (throw (javax.management.openmbean.InvalidKeyException. (str "Invalid key form: " decoded)))
      (list (.substring decoded 0 colon-index) (.substring decoded (inc colon-index))))))

