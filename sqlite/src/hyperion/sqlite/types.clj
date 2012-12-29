(ns hyperion.sqlite.types
  (:require [chee.coerce :refer [->bool]]
            [hyperion.coerce :refer [->long ->biginteger]]
            [hyperion.api :refer [unpack pack]]))

(defmethod pack java.lang.Boolean [_ value]
  (->bool value))

(defmethod unpack java.lang.Boolean [_ value]
  (->bool value))

(defmethod pack java.lang.Long [_ value]
  (->long value))

(defmethod unpack java.lang.Long [_ value]
  (->long value))

(defmethod pack java.math.BigInteger [_ value]
  (when-let [bigint (->biginteger value)]
    (.toString bigint 2)))

(defmethod unpack java.math.BigInteger [_ value]
  (when value
    (cond
      (= BigInteger (type value)) value
      :else (BigInteger. (String. value) 2))))
