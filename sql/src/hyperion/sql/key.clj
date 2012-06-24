(ns hyperion.sql.key)

(defn build-key [table id]
  (str table "-" id))

(defn destructure-key [key]
  (if (nil? key)
    ["" nil]
    (if (.contains key "-")
      (let [index (.lastIndexOf key "-")
            table-name (.substring key 0 index)
            id (Integer/parseInt (.substring key (inc index) (.length key)))]
        [table-name id])
      [key nil])))
