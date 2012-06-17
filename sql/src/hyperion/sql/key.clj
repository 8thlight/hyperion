(ns hyperion.sql.key
  (:use
    [hyperion.sql.format :only [format-as-kind]]))

(defn build-key [table-name id]
  (str (format-as-kind table-name) "-" id))

(defn destructure-key [key]
  (if (nil? key)
    ["" nil]
    (if (.contains key "-")
      (let [index (.lastIndexOf key "-")
            table-name (.substring key 0 index)
            id (Integer/parseInt (.substring key (inc index) (.length key)))]
        [table-name id])
      [key nil])))
