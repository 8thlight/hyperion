(ns hyperion.sql.key
  (:require
    [hyperion.sql.format :refer [format-as-table]]))

(defn build-key [table-name id]
  (str (format-as-table table-name) "-" id))

(defn destructure-key [key]
  (let [index (.lastIndexOf key "-")
        table-name (.substring key 0 index)
        id (Integer/parseInt (.substring key (inc index) (.length key)))]
    [table-name id]))
