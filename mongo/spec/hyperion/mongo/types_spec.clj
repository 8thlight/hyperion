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

  (context "bigintegers"
    (it "packs to a binary string"
      (should= "10" (pack BigInteger (BigInteger. "2")))
      (should= "10" (pack BigInteger (byte 2)))
      (should= "10" (pack BigInteger (short 2)))
      (should= "10" (pack BigInteger (int 2)))
      (should= "10" (pack BigInteger (long 2))))

    (it "unpacks to a big int"
      (should= (BigInteger. "2") (unpack BigInteger "10"))
      (should= (BigInteger. "2") (unpack BigInteger (BigInteger. "2"))))

    )
  )
