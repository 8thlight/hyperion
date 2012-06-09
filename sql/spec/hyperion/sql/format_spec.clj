(ns hyperion.sql.format-spec
  (:require
    [speclj.core :refer :all]
    [chee.datetime :refer [now]]
    [hyperion.sql.format :refer :all]))

(describe "Sql Formatting"
  (context "formats as a table"
    (it "for a string"
      (should= "table" (format-as-table "table")))

    (it "for a keyword"
      (should= "table" (format-as-table :table))))

  (context "formats as a column"
    (it "for a string"
      (should= "value" (format-as-column "value")))

    (it "for a keyword"
      (should= "thing" (format-as-column :thing)))

    (it "for a list"
      (should= "(value, value)" (format-as-column (list "value" :value))))

    (it "for a vector"
      (should= "(value, value)" (format-as-column ["value" :value]))))

  (context "formats as an operator"
    (it "for a string"
      (should= "=" (format-as-column "=")))

    (it "for a keyword"
      (should= "=" (format-as-column :=))))

  (context "formats as a value"
    (it "for a string"
      (should= "'value'" (format-as-value "value")))

    (it "for a number"
      (should= "8" (format-as-value 8)))

    (it "for a keyword"
      (should= "'thing'" (format-as-value :thing)))

    (it "for a list"
      (should= "(1, 'value', 'value')" (format-as-value (list 1 "value" :value))))

    (it "for a vector"
      (should= "(1, 'value', 'value')" (format-as-value [1 "value" :value])))

    (it "for a date"
      (let [date (now)]
        (should= (str "'" date "'") (format-as-value date))))

    (it "for a nil"
      (should= "NULL" (format-as-value nil)))))
