(ns hyperion.sql.format
  (:use
    [chee.string :only [snake-case spear-case]]
    [hyperion.sql.key :only [build-key]])
  (:require
    [clojure.string :as str]))

(defn- add-quotes [s quote]
  (str quote s quote))

(defprotocol FormattableForDatabase
  (table->db [this quote])
  (column->db [this quote])
  (operator->db [this])
  (record->db [this])
  (record<-db [this table] [this table id]))

(extend-protocol FormattableForDatabase
  java.lang.String
  (table->db [this quote]
    (add-quotes (snake-case this) quote))

  clojure.lang.Keyword
  (operator->db [this] (name this))

  (column->db [this quote]
    (add-quotes (snake-case (name this)) quote))

  clojure.lang.Sequential
  (column->db [this quote]
    (str "(" (str/join ", " (map #(column->db % quote) this)) ")"))

  clojure.lang.IPersistentMap
  (record->db [this]
    (dissoc this :id :kind))

  (record<-db
    ([this table id]
      (merge this {:kind table :id id}))
    ([this table]
      (assoc this :kind table))))
