(ns hyperion.sql.format-spec
  (:use
    [speclj.core]
    [chee.datetime :only [now]]
    [hyperion.sql.format]))

(describe "Sql Formatting"
  (context "formats as a table"
    (context "for a string"
      (it "adds quotes"
        (should= "\"table\"" (format-as-table "table")))

      (it "converts dashes to underscores"
        (should= "\"two_worlds\"" (format-as-table "two-worlds")))

      (it "lowercases"
        (should= "\"two\"" (format-as-table "Two"))))

    (context "for a keyword"
      (it "adds quotes"
        (should= "\"table\"" (format-as-table :table)))

      (it "converts dashes to underscores"
        (should= "\"two_worlds\"" (format-as-table :two-worlds)))

      (it "lowercases"
        (should= "\"two\"" (format-as-table :Two)))))

  (context "formats as a column"
    (it "for a string"
      (should= "\"value\"" (format-as-column "value")))

    (it "snake cases"
      (should= "\"value_two\"" (format-as-column "Value-two")))

    (it "for a keyword"
      (should= "\"thing\"" (format-as-column :thing)))

    (it "for a list"
      (should= "(\"value\", \"value\")" (format-as-column (list "value" :value))))

    (it "for a vector"
      (should= "(\"value\", \"value\")" (format-as-column ["value" :value]))))

  (context "formats as an operator"
    (it "for a string"
      (should= "=" (format-as-operator "=")))

    (it "for a keyword"
      (should= "=" (format-as-operator :=))))

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

    (it "for booleans"
      (should= (str "'" true "'") (format-as-value true)))

    (it "for a nil"
      (should= "NULL" (format-as-value nil)))
           
           )

  (it "formats from database"
    (should= {:first-name "sally" :last-name "smith"} (format-record-from-database {:first_name "sally" :last_name "smith"})))

  (it "formats from database"
    (should= {:first_name "sally" :last_name "smith"} (format-record-for-database {:first-name "sally" :last-name "smith"}))))
