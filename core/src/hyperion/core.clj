(ns hyperion.core
  (:require
    [clojure.string :as str]
    [hyperion.sorting :as sort]
    [hyperion.filtering :as filter])
  (:use
    [chee.string :only (gsub spear-case)]
    [chee.datetime :only (now)]
    [chee.util :only (->options)]))

(declare #^{:dynamic true} *ds*)
(def DS (atom nil))

(defprotocol Datastore
  (ds-save [this records])
  (ds-delete-by-key [this key])
  (ds-delete-by-kind [this kind filters])
  (ds-count-by-kind [this kind filters])
  (ds-find-by-key [this key])
  (ds-find-by-kind [this kind filters sorts limit offset])
  (ds-all-kinds [this]))

(defn ds []
  (if (bound? #'*ds*)
    *ds*
    (or @DS (throw (NullPointerException. "No Datastore bound (hyperion/*ds*) or installed (hyperion/DS).")))))

(def #^{:dynamic true} *entity-specs* (ref {}))

(defprotocol Packable
  (pack [this value])
  (unpack [this value]))

(extend-protocol Packable
  Object
  (pack [this value] value)
  (unpack [this value] value)

  nil
  (pack [this value] value)
  (unpack [this value] value))

(defn new? [record]
  (nil? (:key record)))

(defprotocol AsKind
  (->kind [this]))

(extend-protocol AsKind
  java.lang.String
  (->kind [this] (spear-case this))

  clojure.lang.Keyword
  (->kind [this] (->kind (name this)))

  clojure.lang.Symbol
  (->kind [this] (->kind (name this)))

  clojure.lang.IPersistentMap
  (->kind [this] (->kind (:kind this)))

  nil
  (->kind [this] this))

(defprotocol AsKeyword
  (->keyword [this]))

(extend-protocol AsKeyword
  java.lang.String
  (->keyword [this] (keyword this))

  clojure.lang.Keyword
  (->keyword [this] this)

  nil
  (->keyword [this] nil))

(defprotocol AsField
  (->field [this]))

(extend-protocol AsField
  java.lang.String
  (->field [this] (keyword (spear-case this)))

  clojure.lang.Keyword
  (->field [this] (->field (name this)))

  clojure.lang.Symbol
  (->field [this] (->field (name this))))

(defprotocol Specable
  (spec-for [this]))

(extend-protocol Specable
  clojure.lang.IPersistentMap
  (spec-for [this] (spec-for (->kind this)))

  clojure.lang.Keyword
  (spec-for [this] (get @*entity-specs* this))

  java.lang.String
  (spec-for [this] (spec-for (keyword this)))

  nil
  (spec-for [this] this))

(defn- if-assoc [map key value]
  (if value
    (assoc map key value)
    map))

; ----- Hooks ---------------------------------------------

(defmulti after-create #(->keyword (:kind %)))
(defmethod after-create :default [record] record)

(defmulti before-save #(->keyword (:kind %)))
(defmethod before-save :default [record] record)

(defmulti after-load #(->keyword (:kind %)))
(defmethod after-load :default [record] record)

; ----- Entity Implementation -----------------------------

(defmulti pack (fn [type value] type))
(defmethod pack :default [type value] value)

(defmulti unpack (fn [type value] type))
(defmethod unpack :default [type value] value)

(defn- apply-type-packers [options]
  (if-let [t (:type options)]
    (-> (dissoc options :type)
      (assoc :packer t)
      (assoc :unpacker t))
    options))

(defn- map-fields [fields]
  (reduce
    (fn [spec [key & args]]
      (let [options (->options args)
            options (apply-type-packers options)]
        (assoc spec (->field key) options)))
    {}
    fields))

(defn- null-val-fn [field-spec value] value)

(defn create-entity
  ([record] (create-entity record null-val-fn))
  ([record val-fn]
    (let [kind (->kind record)
          spec (spec-for kind)]
      (after-create
        (if spec
          (reduce
            (fn [entity [field spec]]
              (assoc entity field (val-fn spec (field record))))
            (if-assoc {} :kind kind)
            spec)
          (if-assoc record :kind kind))))))

(defn- apply-default [field-spec value]
  (or value (:default field-spec)))

(defn create-entity-with-defaults
  ([record] (create-entity-with-defaults record null-val-fn))
  ([record val-fn]
    (create-entity record #(->> %2 (apply-default %1) (val-fn %1)))))

(defmacro defentity [class-sym & fields]
  (let [field-map (map-fields fields)
        kind (->kind class-sym)]
    `(do
      (dosync (alter *entity-specs* assoc ~(->keyword kind) ~field-map))
      (defn ~(symbol kind) [& args#] (create-entity-with-defaults (assoc (->options args#) :kind ~kind))))))

; ----- Packing / Unpacking -------------------------------

(defn- packer-fn [packer]
  (if (fn? packer)
    packer
    #(pack packer %)))

(defn- unpacker-fn [packer]
  (if (fn? packer)
    packer
    #(unpack packer %)))

(defn- do-packing [packer value]
  (if (or (sequential? value) (isa? (class value) java.util.List))
    (map #(packer %) value)
    (packer value)))

(defn- pack-field [field-spec value]
  (do-packing (packer-fn (:packer field-spec)) value))

(defn- unpack-field [field-spec value]
  (do-packing (unpacker-fn (:unpacker field-spec)) value))

(defn- normalize-fields [record]
  (reduce
    (fn [record [field value]]
      (assoc record (->field field) value))
    {}
    record))

(defn- unpack-entity [record]
  (when record
    (let [record (normalize-fields record)
          entity (create-entity record unpack-field)]
      (if-assoc entity :key (:key record)))))

(defn- pack-entity [record]
  (let [entity (create-entity-with-defaults record pack-field)]
    (if-assoc entity :key (:key record))))

(defn- with-created-at [record spec]
  (if (and (or (contains? spec :created-at) (contains? record :created-at)) (= nil (:created-at record)))
    (assoc record :created-at (now))
    record))

(defn- with-updated-at [record spec]
  (if (or (contains? spec :updated-at) (contains? record :updated-at))
    (assoc record :updated-at (now))
    record))

(defn- with-updated-timestamps [record]
  (let [spec (spec-for record)]
    (-> record
      (with-created-at spec)
      (with-updated-at spec))))

(defn- native->entity [native]
  (-> native
    unpack-entity
    after-load))

(defn- prepare-for-save [entity]
  (-> entity
    pack-entity
    with-updated-timestamps
    before-save))

; ----- API --------------------------------------

(defn save [record & args]
  (let [attrs (->options args)
        record (merge record attrs)
        entity (prepare-for-save record)
        saved (first (ds-save (ds) [entity]))]
    (native->entity saved)))

(defn save* [& records]
  (doall (map native->entity (ds-save (ds) (map prepare-for-save records)))))

(defn- ->filter-operator [operator]
  (case (name operator)
    ("=" "eq") :=
    ("<" "lt") :<
    ("<=" "lte") :<=
    (">" "gt") :>
    (">=" "gte") :>=
    ("!=" "not") :!=
    ("contains?" "contains" "in?" "in") :contains?
    (throw (Exception. (str "Unknown filter operator: " operator)))))

(defn- ->sort-direction [dir]
  (case (name dir)
    ("asc" "ascending") :asc
    ("desc" "descending") :desc
    (throw (Exception. (str "Unknown sort direction: " dir)))))

; Protocol?
(defn- ->seq [items]
  (cond
    (nil? items) []
    (coll? (first items)) items
    :else [items]))

(defn- parse-filters [kind filters]
  (let [spec (spec-for kind)
        filters (->seq filters)]
    (doall (map
      (fn [[operator field value]]
        (let [field (->field field)]
          (filter/make-filter
            (->filter-operator operator)
            (->field field)
            (pack-field (field spec) value))))
      filters))))

(defn- parse-sorts [sorts]
  (let [sorts (->seq sorts)]
    (doall (map
      (fn [[field direction]]
        (sort/make-sort
          (->field field)
          (->sort-direction direction)))
      sorts))))

(defn- find-records-by-kind [kind filters sorts limit offset]
  (map native->entity (ds-find-by-kind (ds) kind (parse-filters kind filters) sorts limit offset)))

(defn find-by-key [key]
  (native->entity
    (ds-find-by-key (ds) key)))

(defn reload [entity]
  (find-by-key (:key entity)))

(defn find-by-kind [kind & args]
  (let [options (->options args)
        kind (name kind)]
    (find-records-by-kind kind
      (:filters options)
      (parse-sorts (:sorts options))
      (:limit options)
      (:offset options))))

(defn find-all-kinds [& args]
  (let [options (->options args)
        kinds (ds-all-kinds (ds))
        sorts (parse-sorts (:sorts options))
        filters (:filters options)
        results (flatten (map #(find-records-by-kind % filters nil nil nil) kinds))]
    (->> results
      (filter #(not (nil? %)))
      (sort/sort-results (parse-sorts (:sorts options)))
      (filter/offset-results (:offset options))
      (filter/limit-results (:limit options)))))

(defn- count-records-by-kind [kind filters]
  (ds-count-by-kind (ds) kind (parse-filters kind filters)))

(defn count-by-kind [kind & args]
  (let [options (->options args)
        kind (name kind)]
    (count-records-by-kind kind (:filters options))))

(defn- count-records-by-all-kinds [filters]
  (let [kinds (ds-all-kinds (ds))
        results (flatten (map #(count-records-by-kind % filters) kinds))]
    (apply + results)))

(defn count-all-kinds [& args]
  (let [options (->options args)]
    (count-records-by-all-kinds (:filters options))))

(defn delete-by-key [key]
  (ds-delete-by-key (ds) key)
  nil)

(defn delete-by-kind [kind & args]
  (let [options (->options args)
        kind (->kind kind)]
    (ds-delete-by-kind (ds) kind (parse-filters kind (:filters options)))
    nil))
