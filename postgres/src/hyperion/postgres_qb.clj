(ns hyperion.postgres-qb
  (:use
    [hyperion.sql.query-builder-fn :only [new-query-builder-fn]]
    [hyperion.sql.sql-qb :only [sql-query-builder-fns]]))

(defn- format-type [pg-type]
  (if (isa? (type pg-type) clojure.lang.Keyword)
    (name pg-type)
    pg-type))

(defn type-cast [value type]
  (if (nil? type)
    value
    (str value "::" (format-type type))))

(def ^:private pg-query-builder-fns (merge sql-query-builder-fns {:type-cast type-cast}))

(defn new-postgres-query-builder [database]
  (new-query-builder-fn (merge pg-query-builder-fns {:database database})))
