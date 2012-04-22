(ns hyperion.mysql
  (:require
    [hyperion.core :refer [Datastore new?]]
    [clojure.java.jdbc :as sql]
    [clojure.string :as clj-str]))

(defmulti format-table type)
(defmethod format-table clojure.lang.Keyword [table] (name table))
(defmethod format-table :default [table] table)

(defmulti format-column type)
(defmethod format-column java.lang.String [val] val)
(defmethod format-column clojure.lang.Keyword [val] (name val))
(defmethod format-column clojure.lang.Sequential [val] (str "(" (clj-str/join ", " (map format-column val)) ")"))

(defmulti format-value type)
(defmethod format-value java.lang.String [val] (str "'" val "'"))
(defmethod format-value clojure.lang.Keyword [val] (name val))
(defmethod format-value clojure.lang.Sequential [val] (str "(" (clj-str/join ", " (map format-value val)) ")"))
(defmethod format-value java.util.Date [val] (format-value (str val)))
(defmethod format-value nil [val] "NULL")
(defmethod format-value :default [val] (str val))

(defn build-filter
  ([filter] (build-filter filter (format-value (first filter))))
  ([filter op] (build-filter (format-value (second filter)) op (last filter)))
  ([col op val] (str col " " op " " (format-value val))))

(defmulti filter->sql first)

(defmethod filter->sql :!= [filter]
  (build-filter filter "<>"))

(defmethod filter->sql :contains? [filter]
  (build-filter filter "IN"))

(defmethod filter->sql :default [filter]
  (build-filter filter))

(defn apply-filters [query filters]
  (if (empty? filters)
    query
    (let [where-clause (str "WHERE " (clj-str/join " AND " (map filter->sql filters)))]
      (str query " " where-clause))))

(defn sort->sql [sort]
  (let [field (format-value (first sort))]
    (case (second sort)
      :asc
        (str field " IS NULL, " field " ASC")
      :desc
        (str field " IS NOT NULL, " field " DESC"))))

(defn apply-sorts [query sorts]
  (if (empty? sorts)
    query
    (let [order-by-clause (str "ORDER BY " (clj-str/join ", " (map sort->sql sorts)))]
      (str query " " order-by-clause))))

(defn apply-limit [query limit]
  (if (nil? limit)
    query
    (str query " LIMIT " limit)))

(defn apply-offset [query offset]
  (if (nil? offset)
    query
    (str query " OFFSET " offset)))

(defn- build-select [return-statement table filters sorts limit offset]
   (->
    (str "SELECT " return-statement " FROM " (format-table table))
    (apply-filters filters)
    (apply-sorts sorts)
    (apply-limit limit)
    (apply-offset offset)))

(defn- build-key [table-name id]
  (str (format-table table-name) "-" id))

(defn- destructure-key [key]
  (let [index (.lastIndexOf key "-")
        table-name (.substring key 0 index)
        id (Integer/parseInt (.substring key (inc index) (.length key)))]
    [table-name id]))

(defn- apply-kind-and-key
  ([record] (apply-kind-and-key record (:kind record) (:id record)))
  ([record table-name](apply-kind-and-key record table-name (:id record)))
  ([record table-name id]
    (assoc record :kind table-name :key (build-key table-name id))))

(defn- update-record [conn record]
  (let [[table-name id] (destructure-key (:key record))
        record (dissoc record :kind :key)
        select-query (build-select "*" table-name [[:= :id id]] nil nil nil)]
    (sql/with-connection conn
      (sql/update-values table-name ["id=?" id] record)
      (let [record (sql/with-query-results results [select-query] (first results))]
        (apply-kind-and-key record table-name id)))))

(defn- insert-record [conn record]
  (let [table-name (format-table (:kind record))
        record (dissoc record :kind)]
    (sql/with-connection conn
      (let [saved-record (first (sql/insert-records table-name record))
            id (:generated_key saved-record)
            select-query (build-select "*" table-name [[:= :id id]] nil nil nil)
            record (sql/with-query-results results [select-query] (first results))]
        (apply-kind-and-key record table-name id)))))

(defn- save-record [conn record]
  (if (new? record)
    (insert-record conn record)
    (update-record conn record)))

(defn- save-records [conn records]
  (doall (map #(save-record conn %) records)))

(defn- delete-record [conn key]
  (let [[table-name id] (destructure-key key)]
    (sql/with-connection conn
      (sql/delete-rows table-name ["id=?" id]))))

(defn- delete-records [conn keys]
  (doseq [key keys]
    (delete-record conn key)))

(defn- find-by-kind [conn kind filters sorts limit offset]
  (let [query (build-select "*" kind filters sorts limit offset)]
    (sql/with-connection conn
      (sql/with-query-results results [query]
        (doall (map #(apply-kind-and-key % kind) results))))))

(defn- find-by-key [conn key]
  (let [[table-name id] (destructure-key key)]
    (first (find-by-kind conn table-name [[:= :id id]] nil nil nil))))

(deftype MySqlDatastore [conn database]
  Datastore
  (ds-save [this record] (save-record conn record))
  (ds-save* [this records] (save-records conn records))
  (ds-delete [this keys] (delete-records conn keys))
  (ds-find-by-kind [this kind filters sorts limit offset]
    (find-by-kind conn kind filters sorts limit offset))
  (ds-find-by-key [this key] (find-by-key conn key)))

(defn new-mysql-datastore [conn database]
  (MySqlDatastore. conn database))
