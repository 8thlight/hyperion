(ns hyperion.sql.types
  (:require [hyperion.coerce :refer [->float]]
            [hyperion.api :refer [unpack pack]]))

(defmethod pack java.lang.Float [_ value]
  (->float value))

(defmethod unpack java.lang.Float [_ value]
  (->float value))
