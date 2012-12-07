(ns hyperion.sql.format
  (:require [chee.string :refer [snake-case spear-case gsub]]
            [hyperion.sql.key :refer [compose-key decompose-key]]
            [clojure.string :as str]))

(defn- add-quotes [s quote]
  (str quote (.replaceAll s quote (str quote quote)) quote))

(defprotocol FormattableForDatabase
  (table->db [this quote])
  (column->db [this quote])
  (column<-db [this])
  (operator->db [this])
  (record->db [this])
  (record<-db [this table] [this table id]))

(defn- format-record<-db [record]
  (reduce
    (fn [formatted-record [column value]]
      (assoc formatted-record (column<-db column) value))
    {}
    record))

(extend-protocol FormattableForDatabase

  nil
  (column->db [this _] "()")

  java.lang.String
  (table->db [this quote]
    (add-quotes (snake-case this) quote))
  (column<-db [this] (keyword (spear-case this)))

  clojure.lang.Keyword
  (operator->db [this] (name this))
  (column->db [this quote]
    (add-quotes (snake-case (name this)) quote))
  (column<-db [this] (column<-db (name this)))

  clojure.lang.Sequential
  (column->db [this quote]
    (str "(" (str/join ", " (map #(column->db % quote) this)) ")"))

  clojure.lang.IPersistentMap
  (record->db [this]
    (dissoc this :key :kind ))

  (record<-db
    ([this table id]
      (format-record<-db (assoc (dissoc this :id "id") :kind table :key (compose-key table id))))
    ([this table]
      (let [id (or (:id this) (get this "id"))]
        (record<-db this table id))))
  )
