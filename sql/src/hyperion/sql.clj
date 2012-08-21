(ns hyperion.sql
  (:require [clojure.string :as string]
            [hyperion.core :refer [Datastore new? ds-delete-by-kind ds-find-by-kind]]
            [hyperion.sql.query-builder :refer :all ]
            [hyperion.sql.query :refer [make-query]]
            [hyperion.sql.jdbc :refer :all ]
            [hyperion.sql.format :refer [record->db record<-db]]
            [hyperion.sql.key :refer [decompose-key compose-key]]
            [hyperion.filtering :as filter]))

(defprotocol DBStrategy
  (get-count [this result])
  (process-result-record [this result given])
  (table-listing-query [this]))

(defn- update-record [qb kind record]
  (execute-write (build-update qb kind (:id record) (record->db record))))

(defn- insert-record [qb kind record]
  (let [row (record->db record)]
      (execute-write (build-insert qb kind row))))

(defn- save-record [db qb record]
  (let [kind (:kind record)
        result
        ((if (new? record)
           insert-record
           update-record) qb kind record)
        result (process-result-record db result record)]
    (record<-db result kind)))

(deftype SQLDatastore [db qb]
  Datastore

  (ds-save [this records]
    (doall (map #(save-record db qb %) records)))

  (ds-delete-by-key [this key]
    (let [[kind id] (decompose-key key)]
      (ds-delete-by-kind this kind [(filter/make-filter := :id id)])))

  (ds-delete-by-kind [this kind filters]
    (execute-mutation (build-delete qb kind filters)))

  (ds-count-by-kind [this kind filters]
    (let [results (execute-query (build-select qb "COUNT(*)" kind filters nil nil nil))]
      (get-count db (first results))))

  (ds-find-by-key [this key]
    (let [[kind id] (decompose-key key)]
      (first (ds-find-by-kind this kind [(filter/make-filter := :id id)] nil nil nil))))

  (ds-find-by-kind [this kind filters sorts limit offset]
    (let [query (build-select qb "*" kind filters sorts limit offset)
          results (execute-query query)]
      (map #(record<-db % kind) results)))

  (ds-all-kinds [this]
    (let [results (execute-query (make-query (table-listing-query db)))]
      (map #(get % "table_name") results)))

  (ds-pack-key [this value] (decompose-key value))

  (ds-unpack-key [this value] (apply compose-key value))

  )

(defn new-sql-datastore [db qb]
  (SQLDatastore. db qb))
