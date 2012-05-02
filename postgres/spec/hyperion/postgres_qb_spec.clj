(ns hyperion.postgres-qb-spec
  (:use
    [speclj.core]
    [hyperion.sql.query-builder]
    [hyperion.postgres-qb :only [new-postgres-query-builder]]))

(describe "Postgres Query Builder"
  (with qb (new-postgres-query-builder "hyperion"))

  (context "select"
    (it "builds returns with a column names"
      (should= "SELECT thing FROM table"
        (select @qb nil [:thing] :table nil nil nil nil)))

    (it "builds returns with multiple column names"
      (should= "SELECT thing, thing1 FROM table"
        (select @qb nil [:thing :thing1] :table nil nil nil nil)))

    (it "builds returns with an alias"
      (should= "SELECT thing::date AS other FROM table"
        (select @qb nil [[:thing :other :date]] :table nil nil nil nil)))

    (it "builds returns with multiple aliases"
      (should= "SELECT thing::text AS other, thing::int AS thing2 FROM table"
        (select @qb nil [[:thing :other :text] [:thing :thing2 :int]] :table nil nil nil nil)))

    (it "builds returns with aliases and column names"
      (should= "SELECT thing, thing::text AS thing2 FROM table"
        (select @qb nil [:thing [:thing :thing2 :text]] :table nil nil nil nil))))

  (context "select all"
    (it "builds withs without types"
      (should= (str "WITH thing AS (something-fancy) SELECT * FROM thing")
        (select-all @qb [[:thing "something-fancy"]] "thing" nil nil nil nil)))

    (it "builds withs with types"
      (should= (str "WITH thing AS (something-fancy) SELECT * FROM thing")
        (select-all @qb [[:thing "something-fancy"]] "thing" nil nil nil nil))))

  (context "column listing"
    (it "builds the column listing query"
      (should= "SELECT tables.table_name, column_name, data_type FROM information_schema.columns AS columns, (SELECT table_name FROM information_schema.tables WHERE table_schema = 'hyperion') AS tables WHERE columns.table_name = tables.table_name"
        (column-listing @qb)))))
