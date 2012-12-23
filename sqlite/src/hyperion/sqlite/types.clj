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

(defmethod unpack java.lang.Boolean [_ value]
  (when value
    (= value 1)))
