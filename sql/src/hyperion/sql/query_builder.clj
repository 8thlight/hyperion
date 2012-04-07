(ns hyperion.sql.query-builder)

(defprotocol QueryBuilder
  (insert [this table item])
  (update [this table item])
  (delete [this table filters])
  (select [this withs returns table-name filters sorts limit offset])
  (select-all [this withs table filters sorts limit offset])
  (count-all [this withs table filters])
  (union-all [this queries])
  (column-listing [this]))

