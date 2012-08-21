(ns hyperion.sql-spec
  (:require [speclj.core :refer :all]
            [hyperion.core :refer :all]
            [hyperion.sql :refer :all]
            [hyperion.sql.key :refer [compose-key]]
            [hyperion.sql.query-builder :refer [new-query-builder QueryBuilderStrategy]]
            [hyperion.sql.jdbc :refer [execute-write execute-mutation execute-query]]))

(deftype FakeDBStrategy [log]
  DBStrategy
  (get-count [this result] (swap! log conj [:get-count result]) 42)
  (process-result-record [this result given] (swap! log conj [:process-result-record result given]) result)
  (table-listing-query [this] (swap! log conj [:table-listing-query]) "table-listing-query"))

(deftype FakeQueryBuilderStrategy [log]
  QueryBuilderStrategy
  (quote-tick [this] (swap! log conj [:quote-tick]) "'")
  (apply-limit-and-offset [this query limit offset] (swap! log conj [:apply-limit-and-offset query limit offset]) query))

(describe "Hyperion SQL"

  (with log (atom []))
  (with db-strategy (FakeDBStrategy. (atom [])))
  (with qb-strategy (FakeQueryBuilderStrategy. (atom [])))
  (with qb (new-query-builder @qb-strategy))
  (with db (new-sql-datastore @db-strategy @qb))
  (around [it]
    (with-redefs [execute-write (fn [query] (swap! @log conj [:write query]) {:write-response 42 :id 42})
                  execute-mutation (fn [query] (swap! @log conj [:mutation query]) :mutation-response)
                  execute-query (fn [query] (swap! @log conj [:query query]) [{:query-response 42 :id 42}])]
      (it)))

  (it "saves"
    (ds-save @db [{:kind "foo" :value 1}])
    (should= :write (first (first @@log))))

  (it "deletes"
    (ds-delete-by-key @db (compose-key "foo" 123))
    (should= :mutation (first (first @@log)))
    (should-not= -1 (.indexOf (apply str (first @@log)) "123")))

  (it "finds-by-kind"
    (ds-find-by-key @db (compose-key "foo" 123))
    (should= :query (first (first @@log)))
    (should-not= -1 (.indexOf (apply str (first @@log)) "123")))

  )