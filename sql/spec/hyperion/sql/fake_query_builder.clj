(ns hyperion.sql.fake-query-builder
  (:require [hyperion.sql.query-builder :refer [QueryBuilderStrategy]]))

(deftype FakeQueryBuilderStrategy [log]
  QueryBuilderStrategy
  (quote-tick [this] (swap! log conj [:quote-tick]) "'")
  (apply-limit-and-offset [this query limit offset] (swap! log conj [:apply-limit-and-offset query limit offset]) query)
  (empty-insert-query [this] (swap! log conj [:empty-insert-query]) "INSERT INTO %s NOTHING"))

(defn new-fake-query-builder-strategy []
  (FakeQueryBuilderStrategy. (atom [])))