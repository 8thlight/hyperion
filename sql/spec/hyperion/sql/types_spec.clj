(ns hyperion.sql.types-spec
  (:require [speclj.core :refer :all]
            [hyperion.sql.types]
            [hyperion.api :refer [unpack pack]]))

(describe "Sql types"

  (context "floats"
    (it "unpacks a float"
      (let [unpacked (unpack Float (double 1.1))]
        (should= (float 1.1) unpacked)
        (should= Float (type unpacked))))

    (it "packs a float"
      (let [packed (pack Float (double 1.1))]
        (should= (float 1.1) packed)
        (should= Float (type packed))))

    )

  (context "byte"
    (it "unpacks a byte"
      (let [unpacked (unpack Byte (int 1))]
        (should= (byte 1) unpacked)
        (should= Byte (type unpacked))))

    (it "packs a byte"
      (let [packed (pack Byte (int 1))]
        (should= (byte 1) packed)
        (should= Byte (type packed))))

    )

  )
