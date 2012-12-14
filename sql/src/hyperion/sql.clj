(ns hyperion.sql
  (:require [clojure.string :as string]
            [hyperion.abstr :refer [Datastore ds-delete-by-kind ds-find-by-kind]]
            [hyperion.api :refer [new?]]
            [hyperion.sql.connection :refer [with-connection]]
            [hyperion.sql.query-builder :refer :all ]
            [hyperion.sql.query :refer [make-query]]
            [hyperion.sql.jdbc :refer :all ]
            [hyperion.sql.format :refer [record->db record<-db]]
            [hyperion.sql.key :refer [decompose-key compose-key]]
            [hyperion.filtering :as filter]
            [hyperion.log :as log]
            [chee.util :refer [->options]]))

(defprotocol DBStrategy
  (get-count [this result])
  (process-insert-result-record [this result given])
  (process-update-result-record [this result given id])
  (table-listing-query [this]))

(defn- update-record [db qb kind record]
  (let [id (last (decompose-key (:key record)))
        result (execute-write (build-update qb kind id (record->db record)))]
    (process-update-result-record db result record id)))

(defn- insert-record [db qb kind record]
  (let [row (record->db record)
        result (execute-write (build-insert qb kind row))]
    (process-insert-result-record db result record)))

(defn- save-record [db qb record]
  (let [kind (:kind record)
        result ((if (new? record)
                  insert-record
                  update-record) db qb kind record)]
    (record<-db result kind)))

(defn- find-by-key [ds key]
  (try
    (let [[kind id] (decompose-key key)]
      (first (ds-find-by-kind ds kind [(filter/make-filter := :id id)] nil 1 nil)))
    (catch Exception e
      (log/warn (format "find-by-key error: %s" (.getMessage e)))
      nil)))

(deftype SQLDatastore [connection-url db qb]
  Datastore

  (ds-save [this records]
    (with-connection connection-url
      (doall (map #(save-record db qb %) records))))

  (ds-delete-by-key [this key]
    (try
      (let [[kind id] (decompose-key key)]
        (ds-delete-by-kind this kind [(filter/make-filter := :id id)]))
      (catch Exception e
        (log/warn (format "delete-by-key error: %s" (.getMessage e)))
        nil)))

  (ds-delete-by-kind [this kind filters]
    (with-connection connection-url (execute-mutation (build-delete qb kind filters))))

  (ds-count-by-kind [this kind filters]
    (with-connection connection-url
      (let [results (execute-query (build-select qb "COUNT(*)" kind filters nil nil nil))]
        (get-count db (first results)))))

  (ds-find-by-key [this key] (find-by-key this key))

  (ds-find-by-kind [this kind filters sorts limit offset]
    (with-connection connection-url
      (let [query (build-select qb "*" kind filters sorts limit offset)
            results (execute-query query)]
        (map #(record<-db % kind) results))))

  (ds-all-kinds [this]
    (with-connection connection-url
      (let [results (execute-query (make-query (table-listing-query db)))]
        (map #(get % "table_name") results))))

  (ds-pack-key [this value] (second (decompose-key value)))

  (ds-unpack-key [this kind value] (compose-key kind value))

  )

(defn new-sql-datastore [& args]
  (let [options (->options args)]
    (SQLDatastore. (:connection-url options) (:db options) (:qb options))))
