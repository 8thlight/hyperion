(ns hyperion.sql.query-builder-spec
  (:require [speclj.core :refer :all ]
            [hyperion.sql.query-builder :refer :all ]
            [hyperion.sql.fake-query-builder :refer [new-fake-query-builder-strategy]]))

(describe "Query Builder"

  (with strategy (new-fake-query-builder-strategy))
  (with query-builder (new-query-builder @strategy))

  (it "builds empty insert"
    (should= ["INSERT INTO 'foo' NOTHING" []] (build-insert @query-builder "foo" {})))

  (it "builds insert with fields"
    (should= ["INSERT INTO 'foo' ('name') VALUES (?)" ["Joe"]] (build-insert @query-builder "foo" {:name "Joe"})))

  (it "build filters"
    (should= ["SELECT projection FROM 'cat' WHERE 'color' = ?" ["blue"]] (build-select @query-builder "projection" "cat" [[:= :color "blue"]] nil nil nil)))

  (it "inequality queries include NULL"
    (should= ["SELECT projection FROM 'cat' WHERE ('color' <> ? OR 'color' IS NULL)" ["blue"]] (build-select @query-builder "projection" "cat" [[:!= :color "blue"]] nil nil nil)))

  (it "build filters checking for equality with nil"
    (should= ["SELECT projection FROM 'cat' WHERE 'color' IS NULL" []] (build-select @query-builder "projection" "cat" [[:= :color nil]] nil nil nil))
    (should= ["SELECT projection FROM 'cat' WHERE 'color' IS NOT NULL" []] (build-select @query-builder "projection" "cat" [[:!= :color nil]] nil nil nil)))

  )
