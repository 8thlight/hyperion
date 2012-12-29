(ns hyperion.riak.types
  (:require [chee.coerce :refer [->string ->int]]
            [hyperion.coerce :refer [->float ->double ->long ->byte ->biginteger]]
            [hyperion.api :refer [pack unpack]])
  (:import  [java.math BigInteger]))

(defmethod pack java.lang.Number [_ value]
  (->string value))

(defmethod unpack java.lang.Byte [_ value]
  (->byte value))

(defmethod unpack java.lang.Integer [_ value]
  (->int value))

(defmethod unpack java.math.BigInteger [_ value]
  (->biginteger value))

(defmethod unpack java.lang.Long [_ value]
  (->long value))

(defmethod unpack java.lang.Float [_ value]
  (->float value))

(defmethod unpack java.lang.Double [_ value]
  (->double value))
