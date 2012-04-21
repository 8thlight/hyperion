(ns hyperion.mysql
  (:require
    [hyperion.core :refer [Datastore new?]]
    [clojure.java.jdbc :as sql]))

(defmulti format-table type)
(defmethod format-table clojure.lang.Keyword [table] (name table))
(defmethod format-table :default [table] table)

(defn- build-key [table-name id]
  (str (format-table table-name) "-" id))

(defn- destructure-key [key]
  (let [index (.lastIndexOf key "-")
        table-name (.substring key 0 index)
        id (Integer/parseInt (.substring key (inc index) (.length key)))]
    [table-name id]))

(defn- apply-kind-and-key
  ([record] (apply-kind-and-key record (:kind record) (:id record)))
  ([record table-name](apply-kind-and-key record table-name (:id record)))
  ([record table-name id]
    (assoc record :kind table-name :key (build-key table-name id))))

(defn- update-record [conn record]
  (let [[table-name id] (destructure-key (:key record))
        record (dissoc record :kind :key)]
    (sql/with-connection conn
      (sql/update-values table-name ["id=?" id] record))
    (apply-kind-and-key record table-name id)))

(defn- insert-record [conn record]
  (let [table-name (format-table (:kind record))
        record (dissoc record :kind)]
    (sql/with-connection conn
      (let [saved-record (first (sql/insert-records table-name record))
            id (:generated_key saved-record)]
        (apply-kind-and-key record table-name id)))))

(defn- save-record [conn record]
  (if (new? record)
    (insert-record conn record)
    (update-record conn record)))

(deftype MySqlDatastore [conn database]
  Datastore
  (ds-save [this record] (save-record conn record))
  (ds-find-by-kind [this kind filters sorts limit offset] ["temp"]))

(defn new-mysql-datastore [conn database]
  (MySqlDatastore. conn database))
