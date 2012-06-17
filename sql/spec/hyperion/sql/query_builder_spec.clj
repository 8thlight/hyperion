(ns hyperion.sql.query-builder-spec
  (:use
    [speclj.core]
    [hyperion.sql.query-builder]))

(describe "Sql Query Builder"
  (context "filters"
    (context "->sql"
      (it "="
        (should= "\"id\" = 1" (filter->sql [:= :id 1])))

      (it "= with string"
        (should= "\"id\" = '1'" (filter->sql [:= :id "1"])))

      (it "!="
        (should= "\"id\" <> 1" (filter->sql [:!= :id 1])))

      (it "<"
        (should= "\"id\" < 1" (filter->sql [:< :id 1])))

      (it "<="
        (should= "\"id\" <= 1" (filter->sql [:<= :id 1])))

      (it ">"
        (should= "\"id\" > 1" (filter->sql [:> :id 1])))

      (it ">="
        (should= "\"id\" >= 1" (filter->sql [:>= :id 1])))

      (it "contains?"
        (should= "\"id\" IN (1, 2, 3)" (filter->sql [:contains? :id [1, 2, 3]])))

      (it "contains? with strings"
        (should= "\"name\" IN ('sue', 'jane')" (filter->sql [:contains? :name ["sue" "jane"]]))))

    (context "applies filters to a query"
      (it "applies no filters"
        (should= "query" (apply-filters "query" []))
        (should= "query" (apply-filters "query" nil)))

      (it "applies one filter"
        (should= "query WHERE \"id\" = 1"
                 (apply-filters "query" [[:= :id 1]])))

      (it "applies multiple filters"
        (should= "query WHERE \"id\" = 1 AND \"name\" > 'first'"
                 (apply-filters "query" [[:= :id 1] [:> :name "first"]])))))

  (context "apply kind and key"
    (it "applies kind and key to a record"
      (should= {:kind "hat" :key "hat-2"} (apply-kind-and-key {} "hat" 2)))

    (it "applies the kind for string"
      (should= {:kind "hat" :key "hat-2"} (apply-kind-and-key {:id 2} "hat")))

    (it "applies the kind for keyword"
      (should= {:kind "hat" :key "hat-2"} (apply-kind-and-key {:id 2} :hat)))

    (it "builds the key for kind as string"
      (should= {:kind "hat" :key "hat-2"} (apply-kind-and-key {:id 2 :kind "hat"})))

    (it "builds the key for kind as keyword"
      (should= {:kind "hat" :key "hat-2"} (apply-kind-and-key {:id 2 :kind :hat})))))
