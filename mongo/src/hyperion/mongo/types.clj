(ns hyperion.mongo.types
  (:require [hyperion.api :refer [pack unpack]]
            [hyperion.coerce :refer [->float ->byte]]))

(defmethod pack Byte [_ value]
  (->byte value))

(defmethod unpack Byte [_ value]
  (->byte value))

(defmethod pack Float [_ value]
  (->float value))

(defmethod unpack Float [_ value]
  (->float value))
