(ns hyperion.sql.types
  (:require [hyperion.coerce :refer [->float ->byte]]
            [hyperion.api :refer [unpack pack]]))

(defmethod pack java.lang.Float [_ value]
  (->float value))

(defmethod unpack java.lang.Float [_ value]
  (->float value))

(defmethod pack java.lang.Byte [_ value]
  (->byte value))

(defmethod unpack java.lang.Byte [_ value]
  (->byte value))
