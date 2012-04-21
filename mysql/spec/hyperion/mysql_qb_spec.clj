(ns hyperion.mysql-qb-spec
  (:require
    [speclj.core :refer :all]
    [hyperion.sql.query-builder :refer :all]
    [hyperion.mysql-qb :refer [new-mysql-query-builder]]))

(describe "MySQL Query Builder"
  (with qb (new-mysql-query-builder "hyperion"))

  (context "select"
    (it "builds returns with a column names"
      (should= "SELECT thing FROM table"
        (select @qb nil [:thing] :table nil nil nil nil)))

    (it "builds returns with multiple column names"
      (should= "SELECT thing, thing1 FROM table"
        (select @qb nil [:thing :thing1] :table nil nil nil nil)))

    (it "builds returns with an alias"
      (should= "SELECT CAST(thing AS date) AS other FROM table"
        (select @qb nil [[:thing :other :date]] :table nil nil nil nil)))))

