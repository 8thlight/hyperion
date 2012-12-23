(ns hyperion.sql.types-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [unpack pack]]
            [hyperion.sql.types :refer [->float]]))

(describe "Postgres types"

  (context "floats"
    (it "unpacks a float"
      (should= (Float. 1.1) (unpack Float (Double. 1.1))))

    (it "packs a float"
      (should= (Float. 1.1) (pack Float (Double. 1.1))))

    )

  (context "Float"
    (it "coerces a float to a float"
      (should= (Float. "1") (->float (Float. "1"))))

    (it "coerces a string to a float"
      (should= (Float. "1") (->float "1"))
      (should-throw IllegalArgumentException (->float "not an int")))

    (it "coerces a long to a float"
      (should= (Float. "1") (->float (long 1))))

    (it "coerces an integer to a float"
      (should= (Float. "1") (->float (int 1))))

    (it "coerces a double to a float"
      (should= (Float. "1") (->float (double 1))))

    (it "handles nil"
      (should-be-nil (->float nil)))

    )

  )
