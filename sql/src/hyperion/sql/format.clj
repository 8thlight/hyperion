(ns hyperion.sql.format)

(defmulti format-table type)
(defmethod format-table java.lang.String [val] val)
(defmethod format-table clojure.lang.Keyword [val] (name val))

(defmulti format-value type)
;(defmethod format-value clojure.lang.Sequential [val] (str "(" (clj-str/join ", " (map format-value val)) ")"))
;(defmethod format-value java.util.Date [val] (format-value (str val)))
;(defmethod format-value nil [val] "NULL")
(defmethod format-value :default [val] (str val))
