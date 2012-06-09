(ns hyperion.mysql
  (:require
    [clojure.java.jdbc :as sql]
    [clojure.string :as clj-str]
    [clojure.set :as clj-set]
    [hyperion.core :refer [Datastore new?]]
    [hyperion.sql.format :refer :all]
    [hyperion.sql.key :refer :all]
    [hyperion.sql.query-builder :refer :all]))

(defn sort->sql [sort]
  (let [field (format-as-column (first sort))]
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

(defn- build-select [return-statement table filters sorts limit offset]
   (->
    (str "SELECT " return-statement " FROM " (format-as-table table))
    (apply-filters filters)
    (apply-sorts sorts)
    (apply-limit limit)
    (apply-offset offset)))

(defn- update-record [record]
  (let [[table-name id] (destructure-key (:key record))
        record (dissoc record :kind :key)
        select-query (build-select "*" table-name [[:= :id id]] nil nil nil)]
    (sql/update-values table-name ["id=?" id] record)
    (let [record (sql/with-query-results results [select-query] (first results))]
      (apply-kind-and-key record table-name id))))

(defn- insert-record [record]
  (let [table-name (format-as-table (:kind record))
        record (dissoc record :kind)]
    (let [saved-record (first (sql/insert-records table-name record))
          id (:generated_key saved-record)
          select-query (build-select "*" table-name [[:= :id id]] nil nil nil)
          record (sql/with-query-results results [select-query] (first results))]
      (apply-kind-and-key record table-name id))))

(defn- save-record [record]
  (if (new? record)
    (insert-record record)
    (update-record record)))

(defn- save-records [records]
  (doall (map #(save-record %) records)))

(defn- delete-record [key]
  (let [[table-name id] (destructure-key key)]
    (sql/delete-rows table-name ["id=?" id])))

(defn- delete-records [keys]
  (doseq [key keys]
    (delete-record key)))

(defn- find-by-kind [kind filters sorts limit offset]
  (let [query (build-select "*" kind filters sorts limit offset)]
    (sql/with-query-results results [query]
      (doall (map #(apply-kind-and-key % kind) results)))))

(defn- find-by-key [key]
  (let [[table-name id] (destructure-key key)]
    (first (find-by-kind table-name [[:= :id id]] nil nil nil))))

(defn sort-ids-by-table [keys]
  (reduce
    (fn [acc -key]
      (let [[table-name id] (destructure-key -key)]
        (update-in acc [table-name]
          (fn [keys] (if (nil? keys) [id] (cons id keys))))))
    {}
    keys))

(defn find-by-keys [keys]
  (let [records
        (for [[table-name ids] (sort-ids-by-table keys)]
          (find-by-kind table-name [[:in :id ids]] nil nil nil))]
    (->> records
      (flatten)
      (filter #(not (nil? %))))))

(defn- count-records-by-kind [kind filters]
  (let [query (build-select "COUNT(*)" kind filters nil nil nil)]
    (sql/with-query-results results [query]
      ((keyword "count(*)") (first results)))))

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

(defn- build-column-listing [select-fn database-name]
  (let [table-listing-query (select-fn "table_name" :information_schema.tables [[:= :table_schema database-name]] nil nil nil)
        table-listing-subquery (str "(" table-listing-query ") AS tables")]
    (with-redefs [format-as-value format-as-column]
      (select-fn "tables.table_name, column_name, data_type" (str "information_schema.columns AS columns, " table-listing-subquery) [[:= :columns.table_name :tables.table_name]] nil nil nil))))

(defn get-schema-and-distinct-columns [database-name]
  (let [column-listing-query (build-column-listing build-select database-name)]
    (sql/with-query-results results [column-listing-query]
      (parse-column-listing results))))

(defn seq-contains? [coll item]
  (some #(= % item) coll))

(defn build-return [return]
  (if (coll? return)
    (let [[value name type] return]
      (str (format-as-value value) " AS " (format-as-column name)))
    (format-as-column return)))

(defn build-return-statement [returns]
  (clj-str/join ", " (map #(build-return %) returns)))

(defn- build-padded-returns [table-name cols dist-cols]
  (let [diff (clj-set/difference (set dist-cols) (set cols))]
    (cons
      [(name table-name) :table_name :char]
      (map #(if (seq-contains? diff %) (cons nil %) (first %)) dist-cols))))

(defn- build-padded-select [select-fn table cols dist-cols filters]
  (let [returns (build-padded-returns table cols dist-cols)
        return-statement (build-return-statement returns)]
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

(defn- count-records-by-all-kinds [database-name filters]
  (let [[schema dist-cols] (get-schema-and-distinct-columns database-name)
        filtered-union (build-filtered-union-by-all-kinds build-select schema dist-cols filters)
        query (build-select "COUNT(*)" (str "(" filtered-union ") AS filtered") nil nil nil nil)]
    (sql/with-query-results results [query]
      ((keyword "count(*)") (first results)))))

(defn- find-records-by-all-kinds [database-name filters sorts limit offset]
  (let [[schema dist-cols] (get-schema-and-distinct-columns database-name)
        filtered-union (build-filtered-union-by-all-kinds build-select schema dist-cols filters)
        query (build-select "*" (str "(" filtered-union ") AS filtered") nil sorts limit offset)]
    (sql/with-query-results results [query]
      (map #(clean-padding-and-apply-keys % schema) (doall results)))))

(deftype MySqlDatastore [database-name]
  Datastore
  (ds-save [this record] (save-record record))
  (ds-save* [this records] (save-records records))
  (ds-delete [this keys] (delete-records keys))
  (ds-count-by-kind [this kind filters options] (count-records-by-kind kind filters))
  (ds-count-all-kinds [this filters options] (count-records-by-all-kinds database-name filters))
  (ds-find-by-key [this key] (find-by-key key))
  (ds-find-by-keys [this keys] (find-by-keys keys))
  (ds-find-by-kind [this kind filters sorts limit offset options]
    (find-by-kind kind filters sorts limit offset))
  (ds-find-all-kinds [this filters sorts limit offset options]
    (find-records-by-all-kinds database-name filters sorts limit offset)))

(defn new-mysql-datastore [database-name]
  (MySqlDatastore. database-name))
