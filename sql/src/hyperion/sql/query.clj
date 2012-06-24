(ns hyperion.sql.query)

(defn query-str [[query-str _]] query-str)

(defn params [[_ params]] params)

(defn make-query
  ([query-str params] [query-str params])
  ([query-str] (make-query query-str [])))

(defn add-str [query new-str]
  (make-query (str (query-str query) " " new-str) (params query)))

(defn add-params [query new-params]
  (make-query (query-str query) (concat (params query) new-params)))

(defn add-to-query [query new-str new-params]
   (-> query
     (add-str new-str)
     (add-params new-params)))
