(ns hyperion.mysql.types
  (:require [hyperion.coerce :refer [->biginteger]]
            [hyperion.api :refer [unpack pack]]))

(defmethod pack java.math.BigInteger [_ value]
  (when-let [bigint (->biginteger value)]
    (.toString bigint 2)))

(defmethod unpack java.math.BigInteger [_ value]
  (when value
    (cond
      (= BigInteger (type value)) value
      :else (BigInteger. (String. value) 2))))

