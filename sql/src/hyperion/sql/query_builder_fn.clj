(ns hyperion.sql.query-builder-fn
  (:use
    [hyperion.sql.query-builder]))


(deftype QueryBuilderFn [database ins upd del sel sel-all count un-all col-listing type-fn]
  QueryBuilder
  (insert [this table item]
    (ins table item))
  (update [this table item]
    (upd table item))
  (delete [this table filters]
    (del table filters))
  (select [this withs returns table-name filters sorts limit offset]
    (sel withs returns table-name filters sorts limit offset type-fn))
  (select-all [this withs table-name filters sorts limit offset]
    (sel-all withs table-name filters sorts limit offset))
  (count-all [this withs table filters]
    (count withs table filters))
  (union-all [this queries]
    (un-all queries))
  (column-listing [this]
    (col-listing database (fn [& args] (apply select this args)))))

(defn new-query-builder-fn [{:keys [database insert update delete select select-all count-all union-all column-listing type-cast]}]
  (QueryBuilderFn. database insert update delete select select-all count-all union-all column-listing type-cast))
