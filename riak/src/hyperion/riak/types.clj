(ns hyperion.riak.types
  (:require [chee.coerce :refer [->string ->int]]
            [hyperion.coerce :refer [->float ->double ->long ->byte]]
            [hyperion.api :refer [pack unpack]])
  (:import  [java.math BigInteger]))

(defmethod pack java.lang.Byte [_ value]
  (->string value))

(defmethod unpack java.lang.Byte [_ value]
  (->byte value))

(defmethod pack java.lang.Integer [_ value]
  (->string value))

(defmethod unpack java.lang.Integer [_ value]
  (->int value))

(defmethod pack java.lang.Long [_ value]
  (->string value))

(defmethod unpack java.lang.Long [_ value]
  (->long value))

(defmethod pack java.lang.Float [_ value]
  (->string value))

(defmethod unpack java.lang.Float [_ value]
  (->float value))

(defmethod pack java.lang.Double [_ value]
  (->string value))

(defmethod unpack java.lang.Double [_ value]
  (->double value))
