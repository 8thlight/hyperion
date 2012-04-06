(ns hyperion.memory
  (:use
    [hyperion.core]))

(defn- != [a b]
  (not (= a b)))

(defn- create-key []
  (str (java.util.UUID/randomUUID)))

(defn- save-record [ds record]
  (let [record (if (new? record) (assoc record :key (create-key)) record)]
    (dosync
      (alter (.store ds) assoc (:key record) record))
    record))

(defn- find-record-by-key [ds key]
  (get @(.store ds) key))

(defn- delete-records [ds keys]
  (dosync
    (apply alter (.store ds) dissoc keys)))

(defn- ->operator [op value]
  (cond
    (= := op) #(= value %)
    (= :!= op) #(not (= value %))
    (= :contains? op) (let [coll (set value)] #(contains? coll %))
    (= :> op) (if (number? value) #(> % value) #(> (.compareTo % value) 0))
    (= :>= op) (if (number? value) #(>= % value) #(>= (.compareTo % value) 0))
    (= :< op) (if (number? value) #(< % value) #(< (.compareTo % value) 0))
    (= :<= op) (if (number? value) #(<= % value) #(<= (.compareTo % value) 0))))

(defn- spec->filter [spec]
  (let [key (second spec)
        value (last spec)
        operator (->operator (first spec) value)]
    (fn [record] (operator (get record key)))))

(defn- build-filter
  ([kind filter-specs]
    (let [speced-filters (map spec->filter filter-specs)
          all-filters (cons #(= kind (:kind %)) speced-filters)]
      (fn [record] (every? #(% record) all-filters))))
  ([filter-specs]
    (fn [record] (every? #(% record) (map spec->filter filter-specs)))))

(defn- ->compare-fn [spec]
  (let [field (first spec)
        multiplier (if (= :desc (second spec)) -1 1)]
    (fn [a b]
      (* multiplier
        (let [av (get a field)
              bv (get b field)]
          (cond
            (and (nil? av) (nil? bv)) 0
            (nil? av) 1
            (nil? bv) -1
            :else (.compareTo av bv)))))))

(defn- build-comparator [sorts]
  (let [compare-fns (map ->compare-fn sorts)]
    (proxy [java.util.Comparator] []
      (compare [a b]
        (or
          (some
            #(if (zero? %) false %)
            (map (fn [compare-fn] (compare-fn a b)) compare-fns))
          0)))))

(defn- apply-sorts [sorts records]
  (if (empty? sorts)
    records
    (sort (build-comparator sorts) records)))

(defn- apply-offset [offset records]
  (if offset
    (drop offset records)
    records))

(defn- apply-limit [limit records]
  (if limit
    (take limit records)
    records))

(defn- do-query [ds filter-fn sorts limit offset]
  (->> @(.store ds)
    vals
    (filter filter-fn)
    (apply-sorts sorts)
    (apply-offset offset)
    (apply-limit limit)))

(defn- find-records-by-kind [ds kind filters sorts limit offset]
  (do-query ds (build-filter kind filters) sorts limit offset))

(defn- find-records [ds filters sorts limit offset]
  (do-query ds (build-filter filters) sorts limit offset))

(deftype MemoryDatastore [store]
  Datastore
  (ds-save [this record] (save-record this record))
  (ds-save* [this records] (doall (for [record records] (save-record this record))))
  (ds-delete [this keys] (delete-records this keys))
  (ds-count-by-kind [this kind filters] (count (find-records-by-kind this kind filters nil nil nil)))
  (ds-count-all-kinds [this filters] (count (find-records this filters nil nil nil)))
  (ds-find-by-key [this key] (find-record-by-key this key))
  (ds-find-by-kind [this kind filters sorts limit offset] (find-records-by-kind this kind filters sorts limit offset))
  (ds-find-all-kinds [this filters sorts limit offset] (find-records this filters sorts limit offset))
  )

(defn new-memory-datastore
  ([] (MemoryDatastore. (ref {})))
  ([stuff] (MemoryDatastore. (ref stuff))))
