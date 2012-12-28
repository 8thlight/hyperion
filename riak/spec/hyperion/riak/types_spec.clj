(ns hyperion.riak.types-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [pack unpack]]
            [hyperion.riak]))

(describe "riak types"
  (context "bytes"
    (it "packs to a string"
      (let [packed (pack Byte (int 1))]
        (should= "1" packed)
        (should= String (type packed))))

    (it "unpacks to a byte"
      (let [unpacked (unpack Byte "1")]
        (should= (byte 1) unpacked)
        (should= Byte (type unpacked))))

    )

  (context "ints"
    (it "packs to a string"
      (let [packed (pack Integer (long 1))]
        (should= "1" packed)
        (should= String (type packed))))

    (it "unpacks to an int"
      (let [unpacked (unpack Integer "1")]
        (should= 1 unpacked)
        (should= Integer (type unpacked))))

    )

  (context "longs"
    (it "packs to a string"
      (let [packed (pack Long (int 1))]
        (should= "1" packed)
        (should= String (type packed))))

    (it "unpacks to a long"
      (let [unpacked (unpack Long "1")]
        (should= 1 unpacked)
        (should= Long (type unpacked))))

    )

  (context "floats"
    (it "packs to a string"
      (should= "1.0" (pack Double (float 1))))

    (it "unpacks to a float"
      (should= (float 1) (unpack Double (double 1))))

    )

  (context "doubles"
    (it "packs to a string"
      (let [packed (pack Double (float 1.1))]
        (should= "1.1" packed)
        (should= String (type packed))))

    (it "unpacks to a double"
      (let [unpacked (unpack Double "1.1")]
        (should= 1.1 unpacked)
        (should= Double (type unpacked))))

    )
  )
