(ns hyperion.core)

(defn ->options
  "Takes keyword argument and converts them to a map.  If the args are prefixed with a map, the rest of the
  args are merged in."
  [options]
  (if (map? (first options))
    (merge (first options) (apply hash-map (rest options)))
    (apply hash-map options)))

(declare #^{:dynamic true} *ds*)
(def DS (atom nil))

(defprotocol Datastore
  (ds-save [this record])
  (ds-save* [this records])
  (ds-delete [this keys])
  (ds-count-by-kind [this kind filters])
  (ds-count-all-kinds [this filters])
  (ds-find-by-key [this key])
  (ds-find-by-kind [this kind filters sorts limit offset])
  (ds-find-all-kinds [this filters sorts limit offset]))

(defn ds []
  (if (bound? #'*ds*)
    *ds*
    (or @DS (throw (NullPointerException. "No Datastore bound (hyperion/*ds*) or installed (hyperion/DS).")))))

(defn new? [record]
  (nil? (:key record)))

(defn ->key [thing]
  (or (:key thing) thing))

(defn save [record & args]
  (let [attrs (->options args)
        record (merge record attrs)]
    (ds-save (ds) record)))

(defn save* [records]
  (ds-save* (ds) records))

(defn find-by-key [key]
  (ds-find-by-key (ds) (->key key)))

(defn reload [entity-or-key]
  (find-by-key entity-or-key))

(defn delete [& keys] (ds-delete (ds) (map ->key keys)))

(defn- ->filter-operator [operator]
  (case (name operator)
    ("=" "eq") :=
    ("<" "lt") :<
    ("<=" "lte") :<=
    (">" "gt") :>
    (">=" "gte") :>=
    ("!=" "not") :!=
    ("contains?" "in") :contains?
    (throw (Exception. (str "Unknown filter operator: " operator)))))

(defn- ->sort-direction [dir]
  (case (name dir)
    ("asc" "ascending") :asc
    ("desc" "descending") :desc
    (throw (Exception. (str "Unknown sort direction: " dir)))))

(defn- parse-filters [filters]
  (when filters
    (let [filters (if (vector? (first filters)) filters (vector filters))]
      (map
        (fn [[operator field value]]
          [(->filter-operator operator) field value])
        filters))))

(defn- parse-sorts [sorts]
  (when sorts
    (let [sorts (if (vector? (first sorts)) sorts (vector sorts))]
      (map
        (fn [[field direction]]
          [field (->sort-direction direction)])
        sorts))))

(defn find-by-kind [kind & args]
  (let [options (->options args)]
    (ds-find-by-kind (ds) kind
      (parse-filters (:filters options))
      (parse-sorts (:sorts options))
      (:limit options)
      (:offset options))))

(defn count-by-kind [kind & args]
  (let [options (->options args)]
    (ds-count-by-kind (ds) kind (parse-filters (:filters options)))))

(defn find-all-kinds [& args]
  (let [options (->options args)]
    (ds-find-all-kinds (ds)
      (parse-filters (:filters options))
      (parse-sorts (:sorts options))
      (:limit options)
      (:offset options))))

(defn count-all-kinds [& args]
  (let [options (->options args)]
    (ds-count-all-kinds (ds) (parse-filters (:filters options)))))


