(ns hyperion.sql.datastore-spec
  (:use
    [speclj.core]
    [hyperion.sql.datastore]
    [hyperion.sql.query-builder-fn]
    [hyperion.sql.query-executor]))

(deftype QueryExecutorFn [querier mutator]
  QueryExecutor
  (do-query [this query] (querier query))
  (do-command [this query] (mutator query)))

(defn new-query-executor-fn [{:keys [querier mutator]}]
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
            database-ret (atom nil)
            sel-all (fn [& args] (reset! result args))
            qb (new-query-builder-fn {:database "data" :select-all sel-all :union-all (fn [queries] :union-ret) :column-listing (fn [database select-fn] (reset! database-ret database))})
            qe (new-query-executor-fn {:querier (fn [query] @col-listing)})
            ds (new-sql-datastore qe qb)
            _ (find-records-by-all-kinds ds :filters :sorts :limit :offset)
            [[[with-name query]] table-name filters sorts limit offset] @result]
        (should= @database-ret "data")
        (should= :union-ret query)
        (should= table-name with-name)
        (should= nil filters)
        (should= :sorts sorts)
        (should= :limit limit)
        (should= :offset offset)))

    (it "calls select with the correct params"
      (let [results (atom [])
            database-ret (atom nil)
            sel (fn [& args] (swap! results conj args))
            qb (new-query-builder-fn {:database "data" :select sel :union-all #(doall %) :select-all (fn [& args]) :column-listing (fn [database select-fn] (reset! database-ret database))})
            qe (new-query-executor-fn {:querier (fn [query] @col-listing)})
            ds (new-sql-datastore qe qb)
            _ (find-records-by-all-kinds ds :filters nil nil nil)]
        (doseq [[withs rets table filters sorts limit offset] @results]
          (should= @database-ret "data")
          (should= (table @returns) rets)
          (should= (name table) (first (first rets)))
          (should= :filters filters))))))

