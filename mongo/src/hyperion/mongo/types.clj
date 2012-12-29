(ns hyperion.mongo.types
  (:require [hyperion.api :refer [pack unpack]]
            [hyperion.coerce :refer [->float ->byte ->biginteger]]))

(defmethod pack Byte [_ value]
  (->byte value))

(defmethod unpack Byte [_ value]
  (->byte value))

(defmethod pack Float [_ value]
  (->float value))

(defmethod unpack Float [_ value]
  (->float value))

(defmethod pack java.math.BigInteger [_ value]
  (when-let [bigint (->biginteger value)]
    (.toString bigint 2)))

(defmethod unpack java.math.BigInteger [_ value]
  (when value
    (cond
      (= BigInteger (type value)) value
      :else (BigInteger. (String. value) 2))))
