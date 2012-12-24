(ns hyperion.dev.spec.types
  (:require [speclj.core :refer :all]
            [hyperion.dev.spec.types.boolean :refer :all]
            [hyperion.dev.spec.types.numbers :refer :all]))

(defn it-handles-types []
  (list
    (context "booleans"
      (it-handles-booleans))
    (context "integers"
      (tags :int)
      (it-handles-ints))
    (context "longs"
      (tags :long)
      (it-handles-longs))
    (context "floats"
      (tags :float)
      (it-handles-floats))

    ))
