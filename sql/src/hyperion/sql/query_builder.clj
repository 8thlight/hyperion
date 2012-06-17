(ns hyperion.sql.query-builder
  (:use
    [hyperion.sql.key :only [build-key]]
    [hyperion.sql.format])
  (:require
    [clojure.string :as str]))

(defn- build-filter
  ([filter] (build-filter filter (first filter)))
  ([filter operator] (build-filter (second filter) operator (last filter)))
  ([column operator value] (str (format-as-column column) " " (format-as-operator operator) " " (format-as-value value))))

(defmulti filter->sql first)

(defmethod filter->sql :!= [filter]
  (build-filter filter "<>"))

(defmethod filter->sql :contains? [filter]
  (build-filter filter "IN"))

(defmethod filter->sql :default [filter]
  (build-filter filter))

(defn apply-kind-and-key
  ([record] (apply-kind-and-key record (:kind record) (:id record)))
  ([record table-name](apply-kind-and-key record table-name (:id record)))
  ([record table-name id]
    (dissoc (assoc record :kind (format-as-kind table-name) :key (build-key table-name id)) :id)))

(defn apply-filters [query filters]
  (if (empty? filters)
    query
    (let [where-clause (str "WHERE " (str/join " AND " (map filter->sql filters)))]
      (str query " " where-clause))))
