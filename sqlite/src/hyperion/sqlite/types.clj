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
