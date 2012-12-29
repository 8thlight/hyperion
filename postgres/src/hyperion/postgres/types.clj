(ns hyperion.postgres.types
  (:require [hyperion.coerce :refer [->biginteger]]
            [hyperion.api :refer [unpack pack]]))

(defmethod pack java.math.BigInteger [_ value]
  (->biginteger value))

(defmethod unpack java.math.BigInteger [_ value]
  (->biginteger value))
