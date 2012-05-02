(ns hyperion.mysql-qb
  (:require
    [hyperion.sql.query-builder-fn :refer [new-query-builder-fn]]
    [hyperion.sql.sql-qb :refer [sql-query-builder-fns]]))

(defn- format-type [pg-type]
  (if (isa? (type pg-type) clojure.lang.Keyword)
    (name pg-type)
    pg-type))

(defn- type-cast [value type]
  (if (nil? type)
    value
    (str "CAST(" value " AS " (format-type type) ")")))

(def ^:private mysql-query-builder-fns (merge sql-query-builder-fns {:type-cast type-cast}))

(defn new-mysql-query-builder [database]
  (new-query-builder-fn (merge mysql-query-builder-fns {:database database})))

