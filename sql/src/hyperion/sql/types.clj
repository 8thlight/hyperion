(ns hyperion.sql.types
  (:require [hyperion.coerce :refer [->float ->byte ->short ->char]]
            [chee.coerce :refer [->string]]
            [hyperion.api :refer [unpack pack]]))

(defmethod pack java.lang.Byte [_ value]
  (->byte value))

(defmethod unpack java.lang.Byte [_ value]
  (->byte value))

(defmethod pack java.lang.Float [_ value]
  (->float value))

(defmethod unpack java.lang.Float [_ value]
  (->float value))

(defmethod pack java.lang.Short [_ value]
  (->short value))

(defmethod unpack java.lang.Short [_ value]
  (->short value))

(defmethod pack java.lang.Character [_ value]
  (->string value))

(defmethod unpack java.lang.Character [_ value]
  (->char value))
