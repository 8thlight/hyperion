(ns hyperion.sql.query-builder
  (:use
    [clojure.string :only [join]]
    [hyperion.sql.query :only [add-str]]
    [hyperion.sql.format]
    [hyperion.sql.query :only [make-query add-to-query]])
  (:require
    [hyperion.filtering :as filter]
    [hyperion.sorting :as sort]))

(defprotocol QueryBuilderStrategy
  (quote-tick [this])
  (apply-limit-and-offset [this query limit offset])
  (table-listing-query [this]))

(defn- sort->sql [s sort]
  (let [sql-order (case (sort/order sort) :asc "ASC" :desc "DESC")]
    (format "%s %s" (column->db (sort/field sort) (quote-tick s)) sql-order)))

(defn- apply-sorts [query s sorts]
  (if (empty? sorts)
    query
    (let [order-by-clause (str "ORDER BY " (join ", " (map #(sort->sql s %) sorts)))]
      (add-str query order-by-clause))))

(defn- in-variables [seq]
  (str "(" (join ", " (repeat (count seq) "?")) ")"))

(defn- build-filter [s column operator variable]
  (format "%s %s %s" (column->db column (quote-tick s)) (operator->db operator) variable))

(defmulti filter->sql #(filter/operator %2))

(defmethod filter->sql :!= [s filter]
  (build-filter s (filter/field filter) :<> "?"))

(defmethod filter->sql :contains? [s filter]
  (build-filter s (filter/field filter) :IN (in-variables (filter/value filter))))

(defmethod filter->sql :default [s filter]
  (build-filter s (filter/field filter) (filter/operator filter) "?"))

(defn apply-filters [query s filters]
  (if (empty? filters)
    query
    (let [values (flatten (map filter/value filters))
          where-clause (str "WHERE " (join " AND " (map #(filter->sql s %) filters)))]
      (add-to-query query where-clause values))))

(def select-query "SELECT %s FROM %s")

(defn apply-limit-and-offset-fn [s limit offset]
  (fn [query]
    (apply-limit-and-offset s query limit offset)))

(defprotocol QueryBuilder
  (build-select [this projection table filters sorts limit offset])
  (build-insert [this table record])
  (build-update [this table id record])
  (build-delete [this table filters])
  (build-table-listing [this]))

(def ^:private insert-query "INSERT INTO %s %s VALUES %s")

(defn- update-query [quote]
  (str "UPDATE %s SET %s WHERE " quote "id" quote " = ?"))

(def delete-query "DELETE FROM %s")

(deftype QB [strategy]
  QueryBuilder
  (build-select [this projection table filters sorts limit offset]
    (->
      (make-query (format select-query projection (table->db table (quote-tick strategy))))
      (apply-filters strategy filters)
      (apply-sorts strategy sorts)
      ((apply-limit-and-offset-fn strategy limit offset))))

  (build-insert [this table record]
    (let [record (record->db record)
          values (vals record)
          query-vars (in-variables values)
          table-name (table->db table (quote-tick strategy))
          col-names (column->db (keys record) (quote-tick strategy))
          query-str (format insert-query table-name col-names query-vars)]
      (make-query query-str values)))

  (build-update [this table id record]
    (let [record (record->db record)
          values (vals record)
          query-vars (join ", " (map (fn [col] (str (column->db col (quote-tick strategy)) " = ?")) (keys record)))
          query-str (format (update-query (quote-tick strategy)) (table->db table (quote-tick strategy)) query-vars)]
      (make-query query-str (concat values [id]))))

  (build-delete [this table filters]
    (->
      (make-query (format delete-query (table->db table (quote-tick strategy))))
      (apply-filters strategy filters)))

  (build-table-listing [this]
    (make-query (table-listing-query strategy))))

(defn new-query-builder [strategy]
  (QB. strategy))
