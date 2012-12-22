(ns hyperion.sqlite.types-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [unpack]]
            [hyperion.sqlite.types :refer :all]))

(describe "sqlite types"
  (context "boolean"
    (it "unpacks true"
      (should= true (unpack Boolean 1)))

    (it "unpacks false"
      (should= false (unpack Boolean 0)))

    (it "unpacks nil"
      (should-be-nil (unpack Boolean nil)))
    )
  )
