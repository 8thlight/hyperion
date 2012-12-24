(ns hyperion.mongo.types
  (:require [hyperion.api :refer [pack unpack]]
            [hyperion.coerce :refer [->float]]))

(defmethod pack Float [_ value]
  (->float value))

(defmethod unpack Float [_ value]
  (->float value))
