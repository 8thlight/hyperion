(ns hyperion.dev.spec.types
  (:require [speclj.core :refer :all]
            [hyperion.dev.spec.types.boolean :refer :all]
            [hyperion.dev.spec.types.numbers :refer :all]
            [hyperion.dev.spec.types.strings :refer :all]))

(defn it-handles-types []
  (list
    (context "booleans"
      (tags :bool)
      (it-handles-booleans))
    (context "bytes"
      (tags :byte)
      (it-handles-bytes))
    (context "shorts"
      (tags :short)
      (it-handles-shorts))
    (context "integers"
      (tags :int)
      (it-handles-ints))
    (context "longs"
      (tags :long)
      (it-handles-longs))
    (context "floats"
      (tags :float)
      (it-handles-floats))
    (context "doubles"
      (tags :double)
      (it-handles-doubles))
    (context "strings"
      (tags :string)
      (it-handles-strings))
    (context "characters"
      (tags :char)
      (it-handles-characters))
    (context "kewords"
      (tags :keyword)
      (it-handles-keywords))

    ))
