(ns hyperion.sql.query-executor)

(defprotocol QueryExecutor
  (do-query [this query])
  (do-command [this command]))
