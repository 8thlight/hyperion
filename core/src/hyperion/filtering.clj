(ns hyperion.filtering)

(defn operator [[operator _ _]] operator)

(defn field [[_ field _]] field)

(defn value [[_ _ value]] value)

(defn make-filter [operator field value]
  [operator field value])

(defn limit-results [limit results]
  (if limit
    (take limit results)
    results))

(defn offset-results [offset results]
  (if offset
    (drop offset results)
    results))
