(ns hyperion.mysql.types-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [pack unpack]]
            [hyperion.mysql]))

(describe "types"
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
