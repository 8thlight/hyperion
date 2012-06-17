(ns hyperion.mysql
  (:use
    [chee.string :only [snake-case]]
    [hyperion.core :only [Datastore new?]]
    [hyperion.sql.format]
    [hyperion.sql.key]
    [hyperion.sql.query-builder])
  (:require
    [clojure.java.jdbc :as sql]
    [clojure.string :as clj-str]
    [clojure.set :as clj-set]
    [clojure.java.jdbc.internal :as sql-internal])
  (:import
    [com.mysql.jdbc.exceptions.jdbc4 MySQLSyntaxErrorException]
    [java.util.regex Matcher Pattern]))

(reset! quote "`")

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

(defn apply-limit-and-offset [query limit offset]
  (if (and (nil? offset) (nil? limit))
    query
    (let [limit (or limit 18446744073709551615)
          offset (or offset 0)]
      (str query " LIMIT " offset "," limit))))

(defn- build-select [return-statement table filters sorts limit offset]
   (->
    (str "SELECT " return-statement " FROM " (format-as-table table))
    (apply-filters filters)
    (apply-sorts sorts)
    (apply-limit-and-offset limit offset)))

(defn- update-record [record]
  (let [[table-name id] (destructure-key (:key record))
        record (dissoc record :kind :key)
        record (format-record-for-database record)]
    (sql/update-values (snake-case table-name) ["id=?" id] record)
    [table-name id]))

(defn insert-record [record]
  (let [table-name (:kind record)
        record (dissoc record :kind :key)
        record (format-record-for-database record)
        result (first (sql/insert-records (snake-case table-name) record))]
    [table-name (:generated_key result)]))

(defn save-record [record]
  (let [[table-name id]
        ((if (new? record)
           insert-record
           update-record) record)
        select-query (build-select "*" table-name [[:= :id id]] nil nil nil)
        record (sql/with-query-results results [select-query] (first results))]
    (apply-kind-and-key (format-record-from-database record) table-name id)))

(defn- save-records [records]
  (doall (map #(save-record %) records)))

(defn- delete-record [key]
  (let [[table-name id] (destructure-key key)]
    (sql/delete-rows table-name ["id=?" id])))

(defn- delete-records [keys]
  (doseq [key keys]
    (delete-record key)))

(def table-not-exist-pattern (Pattern/compile "Table (.*) doesn't exist"))

(defn table-not-exist-error? [e]
  (let [message (.getMessage e)
        matcher (.matcher table-not-exist-pattern message)]
    (.find matcher)))

(defn- find-by-kind [kind filters sorts limit offset]
  (let [query (build-select "*" kind filters sorts limit offset)]
    (try
      (sql/with-query-results results [query]
        (doall (map #(apply-kind-and-key (format-record-from-database %) kind) results)))
      (catch MySQLSyntaxErrorException e
        (if (table-not-exist-error? e)
          []
          (throw e))))))

(defn- find-by-key [key]
  (let [[table-name id] (destructure-key key)]
    (when-not (empty? table-name)
      (first (find-by-kind table-name [[:= :id id]] nil nil nil)))))

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

(defn column-listing-query [database]
  (str "SELECT `tables`.`table_name`, `column_name`, `data_type` FROM `information_schema`.`columns` AS `columns`, (SELECT `table_name` FROM `information_schema`.`tables` WHERE `table_schema` = " (format-as-value database) ") AS `tables` WHERE `columns`.`table_name` = `tables`.`table_name`"))

(defn get-schema-and-distinct-columns [database-name]
  (let [column-listing-query (column-listing-query database-name)]
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
    (apply-kind-and-key (format-record-from-database record) table-name)))

(defn create-temp-table [name query]
  (let [create-temp-table-query (str "CREATE TEMPORARY TABLE " name " " query)]
    (sql-internal/do-prepared-return-keys* create-temp-table-query nil)))

(defn drop-temp-table [name]
  (let [drop-temp-table-query (str "DROP TABLE " name)]
    (sql-internal/do-prepared-return-keys* drop-temp-table-query nil)))

(defn- do-select-all [projection database-name filters sorts limit offset]
  (let [[schema dist-cols] (get-schema-and-distinct-columns database-name)
        filtered-union (build-filtered-union-by-all-kinds build-select schema dist-cols filters)
        temp-table-name "filtered_union_for_find_by_all_kinds"
        _ (create-temp-table temp-table-name filtered-union)
        query (build-select projection temp-table-name nil sorts limit offset)
        query-results (sql/with-query-results results [query] (doall results))]
    (drop-temp-table temp-table-name)
    [schema query-results]))

(defn- count-records-by-all-kinds [database-name filters]
  (let [[schema results] (do-select-all "COUNT(*)" database-name filters nil nil nil)]
    ((keyword "count(*)") (first results))))

(defn- find-records-by-all-kinds [database-name filters sorts limit offset]
  (let [[schema results] (do-select-all "*" database-name filters sorts limit offset)]
    (map #(clean-padding-and-apply-keys % schema) results)))

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
