(ns hyperion.riak.types-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [pack unpack]]
            [hyperion.riak]))

(describe "riak types"
  (context "floats"
    (it "packs to a double"
      (should= (double 1) (pack Double (float 1))))

    (it "unpacks to a float"
      (should= (float 1) (unpack Double (double 1))))

    )

  (context "longs"
    (it "packs to a long"
      (let [packed (pack Long (int 1))]
        (should= 1 packed)
        (should= Long (type packed))))

    (it "unpacks to a long"
      (let [unpacked (unpack Long (int 1))]
        (should= 1 unpacked)
        (should= Long (type unpacked))))

    )
  )
