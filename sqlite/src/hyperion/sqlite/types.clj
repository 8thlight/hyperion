(ns hyperion.sqlite.types
  (:require [chee.coerce :refer [->bool AsBoolean]]
            [hyperion.api :refer [unpack pack]]))

(extend-protocol AsBoolean
  java.lang.Long
  (->bool [this] (= this 1))

  nil
  (->bool [this] this))

(defmethod pack java.lang.Boolean [_ value]
  (->bool value))

; TODO - replace with ->bool from chee
(defmethod unpack java.lang.Boolean [_ value]
  (when value
    (= value 1)))

; TODO - replace with ->long from chee
(defmethod pack java.lang.Long [_ value]
  (when value
    (long value)))

; TODO - replace with ->long from chee
(defmethod unpack java.lang.Long [_ value]
  (when value
    (long value)))
