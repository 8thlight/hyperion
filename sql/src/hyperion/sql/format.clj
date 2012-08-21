(ns hyperion.sql.format
  (:use [chee.string :only [snake-case spear-case]]
        [hyperion.sql.key :only [compose-key decompose-key]])
  (:require [clojure.string :as str]))

(defn- add-quotes [s quote]
  (str quote s quote))

(defprotocol FormattableForDatabase
  (table->db [this quote])
  (column->db [this quote])
  (operator->db [this])
  (record->db [this])
  (record<-db [this table] [this table id]))

(extend-protocol FormattableForDatabase

  nil
  (column->db [this _] "()")

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
    (dissoc this :key :kind ))

  (record<-db
    ([this table id]
      (merge this {:kind table :key (compose-key table id)}))
    ([this table]
      (let [id (or (:id this) (get this "id"))]
        (assoc this :kind table :key (compose-key table id)))))
  )
