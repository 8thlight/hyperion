(ns hyperion.mysql
  (:require
    [hyperion.core :refer [Datastore new?]]
    [clojure.java.jdbc :as sql]
    [clojure.string :as clj-str]
    [clojure.set :as clj-set]))

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

(defn- count-records-by-kind [conn kind filters]
  (let [query (build-select "COUNT(*)" kind filters nil nil nil)]
    (sql/with-connection conn
      (sql/with-query-results results [query]
        ((keyword "count(*)") (first results))))))

(defn parse-column-listing [column-listing]
  (let [schema (reduce #(assoc %1 (keyword (:table_name %2)) []) {} column-listing)]
    (loop [[{:keys [table_name column_name data_type]} & more] column-listing schema schema dist-cols (sorted-set)]
      (if (nil? table_name)
        [schema dist-cols]
        (let [table (keyword table_name)
              col [(keyword column_name) (keyword data_type)]
              schema (update-in schema [table] #(conj % col))
              dist-cols (conj dist-cols col)]
          (recur more schema dist-cols))))))

(defn- build-column-listing [select-fn database]
  (let [table-listing-query (select-fn "table_name" :information_schema.tables [[:= :table_schema database]] nil nil nil)
        table-listing-subquery (str "(" table-listing-query ") AS tables")]
  (select-fn "tables.table_name, column_name, data_type" (str "information_schema.columns AS columns, " table-listing-subquery) [[:= :columns.table_name :tables.table_name]] nil nil nil)))

(defn get-schema-and-distinct-columns [conn database]
  (let [column-listing-query (build-column-listing build-select database)]
    (sql/with-connection conn
      (sql/with-query-results results [column-listing-query]
        (parse-column-listing results)))))

(defn seq-contains? [coll item]
  (some #(= % item) coll))

(defn build-return [return type-cast-fn]
  (if (coll? return)
    (let [[value name type] return]
      (str (format-value value) " AS " (format-column name)))
    (format-value return)))

(defn build-return-statement [returns type-cast-fn]
  (clj-str/join ", " (map #(build-return % type-cast-fn) returns)))

(defn- build-padded-returns [table-name cols dist-cols]
  (let [diff (clj-set/difference (set dist-cols) (set cols))]
    (cons
      [(name table-name) :table_name :char]
      (map #(if (seq-contains? diff %) (cons nil %) (first %)) dist-cols))))

(defn- build-padded-select [select-fn table cols dist-cols filters]
  (let [returns (build-padded-returns table cols dist-cols)
        return-statement (build-return-statement returns type-cast)]
    (select-fn return-statement table filters nil nil nil)))

(defn- build-union-all [queries]
  (clj-str/join " UNION ALL " (map #(str "(" % ")") queries)))

(defn- build-filtered-union-by-all-kinds [select-fn schema dist-cols filters]
  (let [table-select-queries (map (fn [[table cols]] (build-padded-select select-fn table cols dist-cols filters)) schema)]
    (build-union-all table-select-queries)))

(defn col-names [cols]
  (map first cols))

(defn clean-padding-and-apply-keys [record schema]
  (let [table-name (keyword (:table_name record))
        record (select-keys record (col-names (table-name schema)))]
    (apply-kind-and-key record table-name)))

(defn- count-records-by-all-kinds [conn database filters]
  (let [[schema dist-cols] (get-schema-and-distinct-columns conn database)
        filtered-union (build-filtered-union-by-all-kinds build-select schema dist-cols filters)
        query (build-select "COUNT(*)" (str "(" filtered-union ") AS filtered") nil nil nil nil)]
    (sql/with-connection conn
      (sql/with-query-results results [query]
        ((keyword "count(*)") (first results))))))

(defn- find-records-by-all-kinds [conn database filters sorts limit offset]
  (let [[schema dist-cols] (get-schema-and-distinct-columns conn database)
        filtered-union (build-filtered-union-by-all-kinds build-select schema dist-cols filters)
        query (build-select "*" (str "(" filtered-union ") AS filtered") nil sorts limit offset)]
    (sql/with-connection conn
      (sql/with-query-results results [query]
        (map #(clean-padding-and-apply-keys % schema) (doall results))))))

(deftype MySqlDatastore [conn database]
  Datastore
  (ds-save [this record] (save-record conn record))
  (ds-save* [this records] (save-records conn records))
  (ds-delete [this keys] (delete-records conn keys))
  (ds-count-by-kind [this kind filters] (count-records-by-kind conn kind filters))
  (ds-count-all-kinds [this filters] (count-records-by-all-kinds conn database filters))
  (ds-find-by-key [this key] (find-by-key conn key))
  (ds-find-by-kind [this kind filters sorts limit offset]
    (find-by-kind conn kind filters sorts limit offset))
  (ds-find-all-kinds [this filters sorts limit offset]
    (find-records-by-all-kinds conn database filters sorts limit offset)))

(defn new-mysql-datastore [conn database]
  (MySqlDatastore. conn database))
