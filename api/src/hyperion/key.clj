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
  "The default generator for `random-fodder-seq` is threadsafe for JDK 1.6b73
  or greater (http://bugs.java.com/view_bug.do?bug_id=6379897). Calls to this on
  multiple threads (i.e. `(pmap (fn [_] (take 10 random-fodder-seq)) (range 10))`)
  will have a different seed for each instance  of `java.util.Random`. If on a JDK
  earlier than JDK 1.6b73 there is a chance that there will be multiple instances
  of `java.util.Random` with the same seed causing collisions"
  ([] (random-fodder-seq (java.util.Random.)))
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

