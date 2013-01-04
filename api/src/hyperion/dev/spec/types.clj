(ns hyperion.dev.spec.types
  (:require [speclj.core :refer :all]
            [hyperion.dev.spec.types.boolean :refer :all]
            [hyperion.dev.spec.types.numbers :refer :all]))

(defn it-handles-types []
  (list
    (context "booleans"
      (it-handles-booleans))
    (context "bytes"
      (it-handles-bytes))
    (context "integers"
      (it-handles-ints))
    (context "longs"
      (it-handles-longs))
    (context "floats"
      (it-handles-floats))
    (context "doubles"
      (it-handles-doubles))

    ))
