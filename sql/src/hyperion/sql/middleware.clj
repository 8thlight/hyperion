(ns hyperion.sql.middleware
  (:use
    [hyperion.core :only [*ds*]]
    [hyperion.sql.jdbc :only [transaction]]
    [hyperion.sql.connection :only [with-connection-url]]))

(defn with-connection-and-transaction [handler connection-url]
  (fn [request]
    (with-connection-url connection-url
      (transaction
        (handler request)))))
