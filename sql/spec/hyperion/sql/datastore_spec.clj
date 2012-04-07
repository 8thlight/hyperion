(ns hyperion.sql.datastore-spec
  (:use
    [speclj.core]
    [hyperion.sql.datastore]
    [hyperion.sql.query-builder]
    [hyperion.sql.query-executor]))

(deftype QueryBuilderFn [sel sel-all un-all col-listing]
  QueryBuilder
  (insert [this table item])
  (update [this table item])
  (delete [this table filters])
  (select [this withs returns table-name filters sorts limit offset]
    (sel withs returns table-name filters sorts limit offset))
  (select-all [this withs table-name filters sorts limit offset]
    (sel-all withs table-name filters sorts limit offset))
  (count-all [this withs table filters])
  (union-all [this queries]
    (un-all queries))
  (column-listing [this]
    (col-listing)))

(defn noop [& args])

(defn new-query-builder-fn [{:keys [select select-all union-all column-listing]
                             :or {select noop
                                  select-all noop
                                  union-all noop
                                  column-listing noop}}]
  (QueryBuilderFn. select select-all union-all column-listing))

(deftype QueryExecutorFn [querier mutator]
  QueryExecutor
  (do-query [this query] (querier query))
  (do-command [this query] (mutator query)))


(defn new-query-executor-fn [{:keys [querier mutator]
                              :or {querier noop
                                   mutator noop}}]
  (QueryExecutorFn. querier mutator))

(describe "Sql Datastore"
  (with col-listing [{:table_name "t1" :column_name "item1" :data_type "int"}
                        {:table_name "t1" :column_name "item2" :data_type "thing"}
                        {:table_name "t2" :column_name "item2" :data_type "thing"}
                        {:table_name "t3" :column_name "item3" :data_type "some"}])
  (with returns {:t1 [["t1" :table_name :text] :item1 :item2 [nil :item3 :some]]
                 :t2 [["t2" :table_name :text] [nil :item1 :int] :item2 [nil :item3 :some]]
                 :t3 [["t3" :table_name :text] [nil :item1 :int] [nil :item2 :thing] :item3]})
  (context "keys"
    (it "applies a key with the table name and id with kind as a string"
      (should= "table-1" (build-key "table" 1)))

    (it "applies a key with the table name and id with kind as a keyword"
      (should= "table-1" (build-key :table 1))))


  (context "find by all kinds"
    (it "calls select all with the correct params"
      (let [result (atom nil)
            sel-all (fn [& args] (reset! result args))
            qb (new-query-builder-fn {:select-all sel-all :union-all (fn [queries] :union-ret)})
            qe (new-query-executor-fn {:querier (fn [query] @col-listing)})
            ds (new-sql-datastore qe qb)
            _ (find-records-by-all-kinds ds :filters :sorts :limit :offset)
            [[[with-name query]] table-name filters sorts limit offset] @result]
        (should= :union-ret query)
        (should= table-name with-name)
        (should= nil filters)
        (should= :sorts sorts)
        (should= :limit limit)
        (should= :offset offset)))

    (it "calls select with the correct params"
      (let [results (atom [])
            sel (fn [& args] (swap! results conj args))
            qb (new-query-builder-fn {:select sel :union-all #(doall %)})
            qe (new-query-executor-fn {:querier (fn [query] @col-listing)})
            ds (new-sql-datastore qe qb)
            _ (find-records-by-all-kinds ds :filters nil nil nil)]
        (doseq [[withs rets table filters sorts limit offset] @results]
          (should= (table @returns) rets)
          (should= (name table) (first (first rets)))
          (should= :filters filters))))))

