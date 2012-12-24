(ns hyperion.coerce)

(defprotocol AsFloat
  (->float [this]))

(defprotocol AsDouble
  (->double [this]))

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

(extend-protocol AsDouble
  java.lang.Float
  (->double [this] (.doubleValue this))

  java.lang.Double
  (->double [this] this)

  nil
  (->double [this] nil)

  )
