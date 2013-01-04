(ns hyperion.mysql.types
  (:require [chee.coerce :refer [->keyword]]
            [hyperion.coerce]
            [hyperion.api :refer [unpack pack]]))

(defmethod pack clojure.lang.Keyword [_ value]
  (->keyword value))

(defmethod unpack clojure.lang.Keyword [_ value]
  (->keyword value))

