(ns hyperion.sql.format
  (:use
    [chee.string :refer [snake-case spear-case]])
  (:require
    [clojure.string :as str]))

(def quote (atom "\""))

(defn add-quotes [s]
  (str @quote s @quote))

(defprotocol FormattableAsTable
  (format-as-table [this]))

(defprotocol FormattableAsColumn
  (format-as-column [this]))

(defprotocol FormattableAsOperator
  (format-as-operator [this]))

(defprotocol FormattableAsValue
  (format-as-value [this]))

(defprotocol FormattableAsKind
  (format-as-kind [this]))

(extend-type java.lang.String
  FormattableAsTable
  (format-as-table [this] (add-quotes (snake-case this)))

  FormattableAsColumn
  (format-as-column [this] (add-quotes (snake-case this)))

  FormattableAsOperator
  (format-as-operator [this] this)

  FormattableAsValue
  (format-as-value [this] (str "'" this "'"))

  FormattableAsKind
  (format-as-kind [this] this))

(extend-type clojure.lang.Keyword
  FormattableAsTable
  (format-as-table [this] (format-as-table (name this)))

  FormattableAsColumn
  (format-as-column [this] (format-as-column (name this)))

  FormattableAsOperator
  (format-as-operator [this] (format-as-operator (name this)))

  FormattableAsValue
  (format-as-value [this] (format-as-value (name this)))

  FormattableAsKind
  (format-as-kind [this] (format-as-kind (name this))))

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
  (format-as-value [this] (format-as-value (str this)))

  java.lang.Boolean
  (format-as-value [this] (format-as-value (str this)))

  nil
  (format-as-value [this] "NULL"))

(defn format-record-from-database [record]
  (reduce
    (fn [acc [key value]] (assoc acc (keyword (spear-case (name key))) value))
    {}
    record))

(defn format-record-for-database [record]
  (reduce
    (fn [acc [key value]] (assoc acc (keyword (snake-case (name key))) value))
    {}
    record))


