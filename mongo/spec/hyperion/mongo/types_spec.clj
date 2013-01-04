(ns hyperion.mongo.types-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [unpack pack]]
            [hyperion.mongo]))

(describe "types"
  (context "float"
    (it "packs floats"
      (let [packed (pack Float (int 1))]
        (should= (float 1) packed)
        (should= Float (type packed))))

    (it "packs nil"
      (should-be-nil (pack Float nil)))

    (it "unpacks floats"
      (let [unpacked (unpack Float (int 1))]
        (should= (float 1) unpacked)
        (should= Float (type unpacked))))

    (it "unpacks nil"
      (should-be-nil (unpack Float nil)))

    )

  (context "byte"
    (it "packs a byte"
      (let [packed (pack Byte (int 1))]
        (should= (byte 1) packed)
        (should= Byte (type packed))))

    (it "unpacks a byte"
      (let [unpacked (unpack Byte (int 1))]
        (should= (byte 1) unpacked)
        (should= Byte (type unpacked))))

    )
  )
