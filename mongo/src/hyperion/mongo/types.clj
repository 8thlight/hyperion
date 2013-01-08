(ns hyperion.mongo.types
  (:require [chee.coerce :refer [->keyword ->string]]
            [hyperion.api :refer [pack unpack]]
            [hyperion.coerce :refer [->float ->byte ->biginteger ->short ->char]]))

(defmethod pack Byte [_ value]
  (->byte value))

(defmethod unpack Byte [_ value]
  (->byte value))

(defmethod pack Short [_ value]
  (->short value))

(defmethod unpack Short [_ value]
  (->short value))

(defmethod pack Float [_ value]
  (->float value))

(defmethod unpack Float [_ value]
  (->float value))

(defmethod pack clojure.lang.Keyword [_ value]
  (->string value))

(defmethod unpack clojure.lang.Keyword [_ value]
  (->keyword value))

(defmethod pack java.lang.Character [_ value]
  (->char value))

(defmethod unpack java.lang.Character [_ value]
  (->char value))
