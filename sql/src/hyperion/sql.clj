(ns hyperion.sql
  (:use
    [hyperion.core :only [Datastore new? ds-delete-by-kind ds-find-by-kind]]
    [hyperion.sql.query-builder]
    [hyperion.sql.jdbc]
    [hyperion.sql.format :only [record->db record<-db]])
  (:require
    [hyperion.filtering :as filter]))

(defprotocol DBStrategy
  (get-count [this result])
  (process-result-record [this result given]))

(defn- update-record [qb kind record]
  (execute-write (build-update qb kind (:id record) (record->db record))))

(defn- insert-record [qb kind record]
  (execute-write (build-insert qb kind (record->db record))))

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

  (ds-delete-by-id [this kind id]
    (ds-delete-by-kind this kind [(filter/make-filter := :id id)]))

  (ds-delete-by-kind [this kind filters]
    (execute-mutation (build-delete qb kind filters)))

  (ds-count-by-kind [this kind filters]
    (let [results (execute-query (build-select qb "COUNT(*)" kind filters nil nil nil))]
      (get-count db (first results))))

  (ds-find-by-id [this kind id]
    (first (ds-find-by-kind this kind [(filter/make-filter := :id id)] nil nil nil)))

  (ds-find-by-kind [this kind filters sorts limit offset]
    (let [query (build-select qb "*"  kind filters sorts limit offset)
          results (execute-query query)]
      (map #(record<-db % kind) results)))

  (ds-all-kinds [this]
    (let [results (execute-query (build-table-listing qb))]
      (map #(get % "table_name") results))))

(defn new-sql-datastore [db qb]
  (SQLDatastore. db qb))
