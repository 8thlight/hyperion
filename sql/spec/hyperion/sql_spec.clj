(ns hyperion.sql-spec
  (:require [speclj.core :refer :all]
            [hyperion.core :refer :all]
            [hyperion.sql :refer :all]
            [hyperion.sql.key :refer [compose-key]]
            [hyperion.sql.query-builder :refer [new-query-builder]]
            [hyperion.sql.fake-query-builder]
            [hyperion.sql.jdbc :refer [execute-write execute-mutation execute-query]])
  (:import [hyperion.sql.fake_query_builder FakeQueryBuilderStrategy]))

(deftype FakeDBStrategy [log]
  DBStrategy
  (get-count [this result] (swap! log conj [:get-count result]) 42)
  (process-result-record [this result given] (swap! log conj [:process-result-record result given]) result)
  (table-listing-query [this] (swap! log conj [:table-listing-query]) "table-listing-query"))

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

  (it "gracefully handles empty records"
    (should-not-throw (ds-save @db [{:kind "foo"}])))

  (it "deletes"
    (ds-delete-by-key @db (compose-key "foo" 123))
    (should= :mutation (first (first @@log)))
    (should-not= -1 (.indexOf (apply str (first @@log)) "123")))

  (it "finds-by-kind"
    (ds-find-by-key @db (compose-key "foo" 123))
    (should= :query (first (first @@log)))
    (should-not= -1 (.indexOf (apply str (first @@log)) "123")))

  (it "packs and unpacks keys"
    (let [key (compose-key "foo" 123)]
      (should= ["foo" 123] (ds-pack-key @db key))
      (should= key (ds-unpack-key @db ["foo" 123]))))


  )