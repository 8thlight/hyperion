(ns hyperion.postgres
  (:use
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
    [org.postgresql.util PSQLException]
    [java.util.regex Matcher Pattern]))

(defn- format-type [pg-type]
  (if (isa? (type pg-type) clojure.lang.Keyword)
    (name pg-type)
    pg-type))

(defn- type-cast [value type]
  (if (nil? type)
    value
    (str value "::" (format-type type))))

(defn sort->sql [sort]
  (let [order (case (second sort) :asc "ASC" :desc "DESC")]
    (str (format-as-column (first sort)) " " order)))

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

(defn build-with [[name query]]
  (str (format-as-table name) " AS (" query  ")"))

(defn build-withs [withs]
  (when-not (empty? withs)
    (str "WITH " (clj-str/join ", " (map build-with withs)) " ")))

(defn build-return [return type-cast-fn]
  (if (coll? return)
    (let [[value name type] return]
      (str (type-cast-fn (format-as-value value) type) " AS " (format-as-column name)))
    (format-as-column return)))

(defn build-return-statement [returns type-cast-fn]
  (clj-str/join ", " (map #(build-return % type-cast-fn) returns)))

(defn build-insert [table item]
  (let [table-name (format-as-table table)
        column-names (format-as-column (keys item))
        values (format-as-value (vals item))]
    (str "INSERT INTO " table-name " " column-names " VALUES " values)))

(defn build-update [table item]
  (let [table-name (format-as-table table)
        filters (map (fn [[col val]] [:= col val]) (dissoc item :id))
        set (clj-str/join ", " (map filter->sql filters))
        query (str "UPDATE " table-name " SET " set)]
    (apply-filters query [[:= :id (:id item)]])))

(defn build-delete [table filters]
  (let [table-name (format-as-table table)
        query (str "DELETE FROM " table-name)]
    (apply-filters query filters)))

(defn build-select-no-with [return-statement table filters sorts limit offset]
  (->
    (str "SELECT " return-statement " FROM " (format-as-table table))
    (apply-filters filters)
    (apply-sorts sorts)
    (apply-limit limit)
    (apply-offset offset)))

(defn- build-select [withs return-statement table filters sorts limit offset]
   (->
    (str (build-withs withs) "SELECT " return-statement " FROM " (format-as-table table))
    (apply-filters filters)
    (apply-sorts sorts)
    (apply-limit limit)
    (apply-offset offset)))

(defn select [withs returns table filters sorts limit offset type-cast-fn]
  (build-select withs (build-return-statement returns type-cast-fn) table filters sorts limit offset))

(defn select-all [withs table filters sorts limit offset]
  (build-select withs "*" table filters sorts limit offset))

(defn count-all [withs table filters]
  (build-select withs "COUNT(*)" table filters nil nil nil))

(defn build-union-all [queries]
  (clj-str/join " UNION ALL " (map #(str "(" % ")") queries)))

(defn insert-record [record]
  (let [table-name (:kind record)
        record (dissoc record :kind :key)
        query (build-insert table-name record)]
    [table-name query]))

(defn update-record [record]
  (let [[table-name id] (destructure-key (:key record))
        record (assoc record :id id)
        record (dissoc record :kind :key)
        query (build-update table-name record)]
    [table-name query]))

(defn save-record [record]
  (let [[table-name query]
          ((if (new? record)
            insert-record
            update-record) record)
        result (sql-internal/do-prepared-return-keys* query nil)]
    (apply-kind-and-key (format-record-from-database result) table-name)))

(defn save-records [records]
  (doall (map #(save-record %) records)))

(defn delete-record [table-name id]
  (let [query (build-delete table-name [[:= :id id]])]
    (sql-internal/do-prepared-return-keys* query nil)))

(defn delete-records [keys]
  (doseq [key keys]
    (let [[table-name id] (destructure-key key)]
      (delete-record table-name id))))

(defn count-records-by-kind [kind filters]
  (let [query (count-all nil kind filters)]
    (sql/with-query-results
      results [query]
      (:count (first results)))))

(def table-not-exist-pattern (Pattern/compile "ERROR: relation (.*) does not exist"))

(defn table-not-exist-error? [e]
  (let [message (.getMessage e)
        matcher (.matcher table-not-exist-pattern message)]
    (.find matcher)))

(defn find-records-by-kind [kind filters sorts limit offset]
  (let [query (select-all nil kind filters sorts limit offset)]
    (try
    (sql/with-query-results
      results [query]
      (doall (map #(apply-kind-and-key (format-record-from-database %) kind) results)))
      (catch PSQLException e
        (if (table-not-exist-error? e)
          []
          (throw e))))))

(defn find-record-by-key [key]
  (let [[table-name id] (destructure-key key)]
    (when-not (empty? table-name)
      (first (find-records-by-kind table-name [[:= :id id]] nil nil nil)))))

(defn sort-ids-by-table [keys]
  (reduce
    (fn [acc -key]
      (let [[table-name id] (destructure-key -key)]
        (update-in acc [table-name]
          (fn [keys] (if (nil? keys) [id] (cons id keys))))))
    {}
    keys))

(defn find-records-by-keys [keys]
  (let [records
        (for [[table-name ids] (sort-ids-by-table keys)]
          (find-records-by-kind table-name [[:in :id ids]] nil nil nil))]
    (->> records
      (flatten)
      (filter #(not (nil? %))))))

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

(def column-listing-query
 "SELECT \"tables\".\"table_name\", \"column_name\", \"data_type\" FROM \"information_schema\".\"columns\" AS \"columns\", (SELECT \"table_name\" FROM \"information_schema\".\"tables\" WHERE \"table_schema\" = 'public') AS \"tables\" WHERE \"columns\".\"table_name\" = \"tables\".\"table_name\"")

(defn get-schema-and-distinct-columns []
  (sql/with-query-results
    results [column-listing-query]
    (parse-column-listing results)))

(defn seq-contains? [coll item]
  (some #(= % item) coll))

(defn- build-padded-returns [table-name cols dist-cols]
  (let [diff (clj-set/difference (set dist-cols) (set cols))]
    (cons
      [(name table-name) :table_name :text]
      (map #(if (seq-contains? diff %) (cons nil %) (first %)) dist-cols))))

(defn- build-padded-select [table cols dist-cols filters]
  (let [returns (build-padded-returns table cols dist-cols)]
    (select nil returns table filters nil nil nil type-cast)))

(defn- build-filtered-union-by-all-kinds [schema dist-cols filters]
  (let [table-select-queries (map (fn [[table cols]] (build-padded-select table cols dist-cols filters)) schema)]
    (build-union-all table-select-queries)))

(defn col-names [cols]
  (map first cols))

(defn clean-padding-and-apply-keys [record schema]
  (let [table-name (keyword (:table_name record))
        record (select-keys record (col-names (table-name schema)))]
    (apply-kind-and-key (format-record-from-database record) table-name)))

(defn find-records-by-all-kinds [filters sorts limit offset]
  (let [[schema dist-cols] (get-schema-and-distinct-columns)
        filtered-union (build-filtered-union-by-all-kinds schema dist-cols filters)
        with-name "filtered"
        query (select-all [[with-name filtered-union]] with-name nil sorts limit offset)]
    (sql/with-query-results
      results [query]
      (doall (map #(clean-padding-and-apply-keys % schema) results)))))

(defn count-records-by-all-kinds [filters]
  (let [[schema dist-cols] (get-schema-and-distinct-columns)
        filtered-union (build-filtered-union-by-all-kinds schema dist-cols filters)
        with-name "filtered"
        query (count-all [[with-name filtered-union]] with-name nil)]
    (sql/with-query-results
      results [query]
      (:count (first results)))))

(deftype PostgresDatastore []
  Datastore
  (ds-save [this record] (save-record record))
  (ds-save* [this records] (save-records records))
  (ds-delete [this keys] (delete-records keys))
  (ds-count-by-kind [this kind filters options] (count-records-by-kind kind filters))
  (ds-count-all-kinds [this filters options] (count-records-by-all-kinds filters))
  (ds-find-by-key [this key] (find-record-by-key key))
  (ds-find-by-keys [this key] (find-records-by-keys key))
  (ds-find-by-kind [this kind filters sorts limit offset options] (find-records-by-kind kind filters sorts limit offset))
  (ds-find-all-kinds [this filters sorts limit offset options] (find-records-by-all-kinds filters sorts limit offset)))

(defn new-postgres-datastore []
  (PostgresDatastore.))
