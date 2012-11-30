(ns hyperion.sql.fake-db-strategy
  (:require [hyperion.sql :refer [DBStrategy]]))

(deftype FakeDBStrategy [log]
  DBStrategy
  (get-count [this result] (swap! log conj [:get-count result]) 42)
  (process-insert-result-record [this result given] (swap! log conj [:process-insert-result-record result given]) result)
  (process-update-result-record [this result given id] (swap! log conj [:process-update-result-record result given id]) result)
  (table-listing-query [this] (swap! log conj [:table-listing-query]) "table-listing-query"))

(defn new-fake-db-strategy []
  (FakeDBStrategy. (atom [])))
