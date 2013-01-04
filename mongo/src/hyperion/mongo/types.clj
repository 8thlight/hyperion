(ns hyperion.mongo.types
  (:require [chee.coerce :refer [->keyword ->string]]
            [hyperion.api :refer [pack unpack]]
            [hyperion.coerce :refer [->float ->byte ->biginteger]]))

(defmethod pack Byte [_ value]
  (->byte value))

(defmethod unpack Byte [_ value]
  (->byte value))

(defmethod pack Float [_ value]
  (->float value))

(defmethod unpack Float [_ value]
  (->float value))

(defmethod pack clojure.lang.Keyword [_ value]
  (->string value))

(defmethod unpack clojure.lang.Keyword [_ value]
  (->keyword value))
