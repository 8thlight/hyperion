(ns hyperion.sql.middleware
  (:require [hyperion.api :refer [*ds*]]
            [hyperion.sql.jdbc :refer [transaction]]
            [hyperion.sql.connection :refer [with-connection-url]]))

(defn with-connection-and-transaction [handler connection-url]
  (fn [request]
    (with-connection-url connection-url
      (transaction
        (handler request)))))
