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
  )
