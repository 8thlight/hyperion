(ns hyperion.sql.query-builder-fn-spec
  (:use
    [speclj.core]
    [hyperion.sql.query-builder]
    [hyperion.sql.query-builder-fn]))

(describe "Query Builder Fn"
  (it "calls the given insert function"
    (let [result (atom nil)
          insert-fn (fn [& args] (reset! result args))
          qb (new-query-builder-fn {:insert insert-fn})
          _ (insert qb :table :item)
          [table item] @result]
      (should= :table table)
      (should= :item item)))

  (it "calls the given update function"
    (let [result (atom nil)
          update-fn (fn [& args] (reset! result args))
          qb (new-query-builder-fn {:update update-fn})
          _ (update qb :table :item)
          [table item] @result]
      (should= :table table)
      (should= :item item)))

  (it "calls the given delete function"
    (let [result (atom nil)
          delete-fn (fn [& args] (reset! result args))
          qb (new-query-builder-fn {:delete delete-fn})
          _ (delete qb :table :filters)
          [table filters] @result]
      (should= :table table)
      (should= :filters filters)))

  (it "calls the given select function"
    (let [result (atom nil)
          select-fn (fn [& args] (reset! result args))
          type-cast-fn (fn [])
          qb (new-query-builder-fn {:select select-fn :type-cast type-cast-fn})
          _ (select qb :withs :returns :table-name :filters :sorts :limit :offset)
          [withs returns table-name filters sorts limit offset type-cast] @result]
      (should= :withs withs)
      (should= :returns returns)
      (should= :table-name table-name)
      (should= :filters filters)
      (should= :sorts sorts)
      (should= :limit limit)
      (should= :offset offset)
      (should= type-cast type-cast-fn)))

  (it "calls the given select all function"
    (let [result (atom nil)
          select-all-fn (fn [& args] (reset! result args))
          qb (new-query-builder-fn {:select-all select-all-fn})
          _ (select-all qb :withs :table-name :filters :sorts :limit :offset)
          [withs table-name filters sorts limit offset] @result]
      (should= :withs withs)
      (should= :table-name table-name)
      (should= :filters filters)
      (should= :sorts sorts)
      (should= :limit limit)
      (should= :offset offset)))

  (it "calls the given count all function"
    (let [result (atom nil)
          count-all-fn (fn [& args] (reset! result args))
          qb (new-query-builder-fn {:count-all count-all-fn})
          _ (count-all qb :withs :table-name :filters)
          [withs table-name filters] @result]
      (should= :withs withs)
      (should= :table-name table-name)
      (should= :filters filters)))

  (it "calls the given union all function"
    (let [result (atom nil)
          union-all-fn (fn [& args] (reset! result args))
          qb (new-query-builder-fn {:union-all union-all-fn})
          _ (union-all qb :queries)
          [queries] @result]
      (should= :queries queries)))

  (it "calls the given column listing function"
    (let [database-ret (atom nil)
          column-fn (fn [database select]
                      (reset! database-ret database)
                      (select :withs :returns :table-name :filters :sorts :limit :offset))
          type-fn (fn [& args])
          qb (new-query-builder-fn {:database "data" :column-listing column-fn :select (fn [& args] args) :type-cast type-fn})
          [withs returns table-name filters sorts limit offset type-cast] (column-listing qb)]
      (should= @database-ret "data")
      (should= :withs withs)
      (should= :returns returns)
      (should= :table-name table-name)
      (should= :filters filters)
      (should= :sorts sorts)
      (should= :limit limit)
      (should= :offset offset)
      (should= type-fn type-cast))))
