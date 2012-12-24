(ns hyperion.sql.types-spec
  (:require [speclj.core :refer :all]
            [hyperion.sql.types]
            [hyperion.api :refer [unpack pack]]))

(describe "Sql types"

  (context "floats"
    (it "unpacks a float"
      (should= (Float. 1.1) (unpack Float (Double. 1.1))))

    (it "packs a float"
      (should= (Float. 1.1) (pack Float (Double. 1.1))))

    )

  )
