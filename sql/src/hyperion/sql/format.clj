(ns hyperion.sql.format
  (:require
    [clojure.string :as str]))

(defprotocol FormattableAsTable
  (format-as-table [this]))

(defprotocol FormattableAsColumn
  (format-as-column [this]))

(defprotocol FormattableAsOperator
  (format-as-operator [this]))

(defprotocol FormattableAsValue
  (format-as-value [this]))

(extend-type java.lang.String
  FormattableAsTable
  (format-as-table [this] this)

  FormattableAsColumn
  (format-as-column [this] this)

  FormattableAsOperator
  (format-as-operator [this] this)

  FormattableAsValue
  (format-as-value [this] (str "'" this "'")))

(extend-type clojure.lang.Keyword
  FormattableAsTable
  (format-as-table [this] (format-as-table (name this)))

  FormattableAsColumn
  (format-as-column [this] (format-as-column (name this)))

  FormattableAsOperator
  (format-as-operator [this] (format-as-operator (name this)))

  FormattableAsValue
  (format-as-value [this] (format-as-value (name this))))

(extend-type clojure.lang.Sequential
  FormattableAsColumn
  (format-as-column [this]
    (str "(" (str/join ", " (map format-as-column this)) ")"))

  FormattableAsValue
  (format-as-value [this]
    (str "(" (str/join ", " (map format-as-value this)) ")")))

(extend-protocol FormattableAsValue
  java.lang.Number
  (format-as-value [this] (str this))

  clojure.lang.Sequential
  (format-as-value [this]
    (str "(" (str/join ", " (map format-as-value this)) ")"))

  java.util.Date
  (format-as-value [this]
    (format-as-value (str this)))

  nil
  (format-as-value [this] "NULL"))
