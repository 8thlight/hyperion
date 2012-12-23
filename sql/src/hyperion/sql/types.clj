(ns hyperion.sql.types
  (:require [hyperion.api :refer [unpack pack]]))

(defprotocol AsFloat
  (->float [this]))

(extend-protocol AsFloat
  java.lang.String
  (->float [this] (Float. this))

  java.lang.Float
  (->float [this] this)

  java.lang.Long
  (->float [this] (.floatValue this))

  java.lang.Integer
  (->float [this] (.floatValue this))

  java.lang.Double
  (->float [this] (.floatValue this))

  nil
  (->float [this] nil)

  )

(defmethod pack java.lang.Float [_ value]
  (->float value))

(defmethod unpack java.lang.Float [_ value]
  (->float value))
