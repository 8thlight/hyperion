(ns hyperion.sql.format-spec
  (:require
    [speclj.core :refer :all]
    [hyperion.sql.format :refer :all]))

(describe "Sql Formatting"
  (context "formats as a table"
    (it "for a string"
      (should= "table" (format-table "table")))

    (it "for a keyword"
      (should= "table" (format-table :table))))

  (context "formats as a value"
    (it "for a string"
      (should= "\"value\"" (format-value "value")))))
