(ns hyperion.postgres
  (:use [hyperion.core])
  (:require
    [clojure.java.jdbc :as sql]
    [clojure.string :as clj-str]))

(defn- build-key [table-name id]
  (str table-name "-" id))

(defn- destructure-key [key]
  (let [index (.lastIndexOf key "-")
        table-name (.substring key 0 index)
        id (.substring key (inc index) (.length key))]
    [table-name id]))

(defn- apply-kind-and-key
  ([record] (apply-kind-and-key record (:kind record) (:id record)))
  ([record table-name](apply-kind-and-key record table-name (:id record)))
  ([record table-name id]
    (assoc record :kind table-name :key (build-key table-name id))))

(defn- save-record [ds record]
  (sql/with-connection (.conn ds)
    (let [table-name (:kind record)
          to-insert (dissoc record :kind)
          record (sql/insert-record table-name to-insert)
          record (apply-kind-and-key record table-name)]
      record)))

(defn- save-records [ds records]
  (doall (map #(save-record ds %) records)))

(defn- find-record-by-key [ds key]
  (let [[table-name id] (destructure-key key)]
    (sql/with-connection (.conn ds)
      (sql/with-query-results
        res [(str "SELECT * FROM " table-name "  WHERE id=" id)]
        (let [record (first res)]
          (when record
            (apply-kind-and-key record table-name id)))))))

(defn- delete-record [ds table-name id]
  (sql/with-connection (.conn ds)
    (sql/delete-rows
      table-name
      [(str "id=" id)])))

(defn- delete-records [ds keys]
  (doseq [key keys]
    (let [[table-name id] (destructure-key key)]
      (delete-record ds table-name id))))

(defmulti format-value (fn [val] (type val)))

(defmethod format-value java.lang.String [val]
  (str "'" val "'"))

(defmethod format-value clojure.lang.Keyword [val]
  (name val))

(defmethod format-value clojure.lang.Sequential [val]
  (str "(" (clj-str/join ", " (map format-value val)) ")"))

(defmethod format-value :default [val]
  (str val))

(defn- build-filter
  ([filter] (build-filter filter (format-value (first filter))))
  ([filter op] (build-filter (format-value (second filter)) op (last filter)))
  ([col op val] (str col " " op " " (format-value val))))

(defmulti filter->sql (fn [filter] (first filter)))

(defmethod filter->sql :!= [filter]
  (build-filter filter "<>"))

(defmethod filter->sql :contains? [filter]
  (build-filter filter "IN"))

(defmethod filter->sql :default [filter]
  (build-filter filter))

(defn- apply-filters [query filters]
  (if (empty? filters)
    query
    (let [where-clause (str "WHERE " (clj-str/join " AND " (map filter->sql filters)))]
      (str query " " where-clause))))

(defn- sort->sql [sort]
  (let [order (case (second sort) :asc "ASC" :desc "DESC")]
    (str (format-value (first sort)) " " order)))

(defn- apply-sorts [query sorts]
  (if (empty? sorts)
    query
    (let [order-by-clause (str "ORDER BY " (clj-str/join ", " (map sort->sql sorts)))]
      (str query " " order-by-clause))))

(defn- apply-limit [query limit]
  (if (nil? limit)
    query
    (str query " LIMIT " limit)))

(defn- apply-offset [query offset]
  (if (nil? offset)
    query
    (str query " OFFSET " offset)))

(defn- find-records-by-kind [ds kind filters sorts limit offset]
  (let [query (->
                (str "SELECT * FROM " kind)
                (apply-filters filters)
                (apply-sorts sorts)
                (apply-limit limit)
                (apply-offset offset))]
    (sql/with-connection (.conn ds)
      (sql/with-query-results
        results [query]
        (let [full-results (doall (map #(apply-kind-and-key % kind) results))]
          full-results)))))

(defn- count-records-by-kind [ds kind filters]
  (let [query (->
                (str "SELECT COUNT(*) FROM " kind)
                (apply-filters filters))]
    (sql/with-connection (.conn ds)
      (sql/with-query-results
        results [query]
        (:count (first results))))))

(deftype PostgresDatastore [conn]
  Datastore
  (ds-save [this record] (save-record this record))
  (ds-save* [this records] (save-records this records))
  (ds-delete [this keys] (delete-records this keys))
  (ds-count-by-kind [this kind filters] (count-records-by-kind this kind filters))
  (ds-count-all-kinds [this filters])
  (ds-find-by-key [this key] (find-record-by-key this key))
  (ds-find-by-kind [this kind filters sorts limit offset] (find-records-by-kind this kind filters sorts limit offset))
  (ds-find-all-kinds [this filters sorts limit offset])
  )

(defn new-postgres-datastore [conn]
  (PostgresDatastore. conn))
