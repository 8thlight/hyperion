(ns hyperion.sql.query-builder
  (:require [clojure.string :refer [join]]
            [hyperion.sql.query :refer [add-str]]
            [hyperion.sql.format :refer :all]
            [hyperion.filtering :as flt]
            [hyperion.sorting :as sort]
            [hyperion.sql.query :refer [make-query add-to-query]]))

(defprotocol QueryBuilderStrategy
  (quote-tick [this])
  (apply-limit-and-offset [this query limit offset])
  (empty-insert-query [this]))

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

(defmulti filter->sql #(flt/operator %2))

(defmethod filter->sql :!= [s filter]
  (let [value (flt/value filter)
        field (flt/field filter)
        column (column->db (flt/field filter) (quote-tick s))]
    (if-not (nil? value)
      (list (format "(%s <> ? OR %s IS NULL)" column column) (list value))
      (list (format "%s IS NOT NULL" column)))))

(defmethod filter->sql :contains? [s filter]
  (let [value (flt/value filter)
        variables (in-variables value)]
    (if (some #(nil? %) value)
      (let [column (column->db (flt/field filter) (quote-tick s))]
        (list (format "(%s IN %s OR %s IS NULL)" column variables column) value))
      (list
        (build-filter s (flt/field filter) :IN variables)
        value))))

(defmethod filter->sql := [s filter]
  (if-let [value (flt/value filter)]
    (list (build-filter s (flt/field filter) (flt/operator filter) "?") (list (flt/value filter)))
    (list (format "%s IS NULL" (column->db (flt/field filter) (quote-tick s))))))

(defmethod filter->sql :default [s filter]
  (list (build-filter s (flt/field filter) (flt/operator filter) "?") (list (flt/value filter))))

(defn apply-filters [query s filters]
  (if (empty? filters)
    query
    (let [filter-pairs (map #(filter->sql s %) filters)
          where-clause (str "WHERE " (join " AND " (map first filter-pairs)))]
      (add-to-query query where-clause (flatten (mapcat second filter-pairs))))))

(def select-query "SELECT %s FROM %s")

(defn apply-limit-and-offset-fn [s limit offset]
  (fn [query]
    (apply-limit-and-offset s query limit offset)))

(defprotocol QueryBuilder
  (build-select [this projection table filters sorts limit offset])
  (build-insert [this table record])
  (build-update [this table id record])
  (build-delete [this table filters]))

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
          table-name (table->db table (quote-tick strategy))]
      (if (seq values)
        (make-query
          (format insert-query table-name
            (column->db (keys record) (quote-tick strategy))
            (in-variables values))
          values)
        (make-query (format (empty-insert-query strategy) table-name)))))

  (build-update [this table id record]
    (let [record (record->db record)
          values (vals record)
          query-vars (join ", " (map (fn [col] (str (column->db col (quote-tick strategy)) " = ?")) (keys record)))
          query-str (format (update-query (quote-tick strategy)) (table->db table (quote-tick strategy)) query-vars)]
      (make-query query-str (concat values [id]))))

  (build-delete [this table filters]
    (->
      (make-query (format delete-query (table->db table (quote-tick strategy))))
      (apply-filters strategy filters))))

(defn new-query-builder [strategy]
  (QB. strategy))
