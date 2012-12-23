(ns hyperion.sqlite.types-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [unpack pack]]
            [hyperion.sqlite]))

(describe "sqlite types"
  (context "boolean"
    (it "unpacks true"
      (should= true (unpack Boolean 1)))

    (it "unpacks false"
      (should= false (unpack Boolean 0)))

    (it "unpacks nil"
      (should-be-nil (unpack Boolean nil)))

    (it "packs true"
      (should= true (pack Boolean 1)))

    (it "packs false"
      (should= false (pack Boolean 0)))

    (it "packs nil"
      (should-be-nil (pack Boolean nil)))

    )
  )
