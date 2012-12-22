(ns hyperion.sqlite.types
  (:require [hyperion.api :refer [unpack]]))

(defmethod unpack java.lang.Boolean [_ value]
  (when value
    (= value 1)))
