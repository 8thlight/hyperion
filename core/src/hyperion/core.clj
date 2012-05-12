(ns hyperion.core
  (:require
    [clojure.string :as str])
  (:use
    [chee.string :only (gsub spear-case)]
    [chee.datetime :only (now)]
    [chee.util :only (->options)]))

(declare #^{:dynamic true} *ds*)
(def DS (atom nil))

(defprotocol Datastore
  (ds->kind [this thing])
  (ds->ds-key [this thing])
  (ds->string-key [this thing])
  (ds-save [this record])
  (ds-save* [this records])
  (ds-delete [this keys])
  (ds-count-by-kind [this kind filters options])
  (ds-count-all-kinds [this filters options])
  (ds-find-by-key [this key])
  (ds-find-by-keys [this keys])
  (ds-find-by-kind [this kind filters sorts limit offset options])
  (ds-find-all-kinds [this filters sorts limit offset options])
  (ds-native->entity [this entity])
  (ds-entity->native [this record]))

(defn ds []
  (if (bound? #'*ds*)
    *ds*
    (or @DS (throw (NullPointerException. "No Datastore bound (hyperion/*ds*) or installed (hyperion/DS).")))))

(defn new? [record]
  (nil? (:key record)))

(defn ->key [thing]
  (or (:key thing) thing))

(defn ->kind [thing]
  (or (:kind thing) (ds->kind (ds) thing)))

(defn kind [thing]
  (->kind thing))

; ----- Packing / Unpacking -------------------------------

(defmulti pack (fn [packer value] packer))
(defmethod pack :default [packer value] value)

(defprotocol Packed
  (unpack [this]))

(extend-type nil
  Packed
  (unpack [this] nil))

(defn pack-field [packer value]
  (cond
    (sequential? value) (map #(pack-field packer %) value)
    (fn? packer) (packer value)
    :else (pack packer value)))

(defn unpack-field [unpacker value]
  (cond
    (isa? (class value) java.util.List) (map #(unpack-field unpacker %) value)
    (fn? unpacker) (unpacker value)
    unpacker (unpack value)
    :else value))

; ----- Hooks ---------------------------------------------

(defprotocol AfterCreate
  (after-create [this]))

(defprotocol BeforeSave
  (before-save [this]))

(defprotocol AfterLoad
  (after-load [this]))

(extend-type Object
  AfterCreate
  (after-create [this] this)
  BeforeSave
  (before-save [this] this)
  AfterLoad
  (after-load [this] this))

(extend-type nil
  AfterCreate
  (after-create [_] nil)
  BeforeSave
  (before-save [_] nil)
  AfterLoad
  (after-load [_] nil))

; ----- Entity <-> Record Translation ---------------------

(def #^{:dynamic true} *entity-specs* (ref {}))

(defmulti native->entity kind)

(defmethod native->entity nil [entity]
  nil)

(defn native->specced-entity
  ([entity]
    (let [kind (->kind entity)
          spec (get @*entity-specs* kind)]
      (native->specced-entity entity kind spec)))
  ([entity kind spec]
    (let [key (ds->string-key (ds) entity)
          record ((:*ctor* spec) key)]
      (after-load
        (reduce
          (fn [record [field value]]
            (let [field (keyword field)]
              (assoc record field (unpack-field (:unpacker (field spec)) value))))
          record
          (ds-native->entity (ds) entity))))))

(defn- native->unspecced-entity [entity kind]
  (after-load
    (reduce
      (fn [record entry] (assoc record (keyword (key entry)) (val entry)))
      {:kind kind :key (ds->string-key (ds) entity)}
      (ds-native->entity (ds) entity))))

(defmethod native->entity :default [entity]
  (let [kind (ds->kind (ds) entity)
        spec (get @*entity-specs* kind)]
    (if spec
      (native->specced-entity entity kind spec)
      (native->unspecced-entity entity kind))))

(defprotocol EntityRecord
  (->native [this]))

(defn- unspecced-entity->native [record kind]
  (ds-entity->native (ds) record))

(defn specced-entity->native
  ([record]
    (let [kind (:kind record)
          spec (get @*entity-specs* kind)]
      (specced-entity->native record kind spec)))
  ([record kind spec]
    (ds-entity->native (ds)
      (reduce
        (fn [marshaled [field attrs]]
          (assoc marshaled field (pack-field (:packer (field spec)) (field record))))
        {:key (:key record) :kind (:kind record)}
        (dissoc spec :*ctor*)))))

(extend-type clojure.lang.APersistentMap
  EntityRecord
  (->native [record]
    (let [kind (:kind record)
          spec (get @*entity-specs* kind)]
      (if spec
        (specced-entity->native record kind spec)
        (unspecced-entity->native record kind)))))

; ----- Timestamps ----------------------------------------

(defn- with-created-at [record spec]
  (if (and (or (contains? spec :created-at) (contains? record :created-at)) (= nil (:created-at record)))
    (assoc record :created-at (now))
    record))

(defn- with-updated-at [record spec]
  (if (or (contains? spec :updated-at) (contains? record :updated-at))
    (assoc record :updated-at (now))
    record))

(defn with-updated-timestamps [record]
  (let [spec (get @*entity-specs* (:kind record))]
    (with-updated-at (with-created-at record spec) spec)))

; ----- Raw CRUD API --------------------------------------

(defn- prepare-for-save [entity]
  (-> entity
    with-updated-timestamps
    before-save
    ->native))

(defn save [record & args]
  (let [attrs (->options args)
        record (merge record attrs)
        entity (prepare-for-save record)
        saved (ds-save (ds) entity)]
    (native->entity saved)))

(defn save* [records]
  (->> records
    (map prepare-for-save)
    (ds-save* (ds))
    (map native->entity)))

(defn find-by-key [key]
  (native->entity
    (ds-find-by-key (ds) (->key key))))

(defn find-by-keys [key]
  (map
    native->entity
    (ds-find-by-keys (ds) (filter identity (map ->key key)))))

(defn reload [entity-or-key]
  (find-by-key entity-or-key))

(defn delete [& keys] (ds-delete (ds) (map ->key keys)))

; ----- Searching -----------------------------------------

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
  (let [options (->options args)
        kind (name kind)]
    (map native->entity
      (ds-find-by-kind (ds) kind
        (parse-filters (:filters options))
        (parse-sorts (:sorts options))
        (:limit options)
        (:offset options)
        (dissoc options :filters :sorts :limit :offset)))))

(defn find-all-kinds [& args]
  (let [options (->options args)]
    (map native->entity
      (ds-find-all-kinds (ds)
        (parse-filters (:filters options))
        (parse-sorts (:sorts options))
        (:limit options)
        (:offset options)
        (dissoc options :filters :sorts :limit :offset)))))

(defn count-by-kind [kind & args]
  (let [options (->options args)
        kind (name kind)]
    (ds-count-by-kind (ds) kind (parse-filters (:filters options))
      (dissoc options :filters :sorts :limit :offset))))

(defn count-all-kinds [& args]
  (let [options (->options args)]
    (ds-count-all-kinds (ds) (parse-filters (:filters options))
      (dissoc options :filters :sorts :limit :offset))))

; ----- Entity Implementation -----------------------------

(defn- map-fields [fields]
  (reduce
    (fn [spec [key & args]]
      (let [attrs (apply hash-map args)
            attrs (if-let [t (:type attrs)] (assoc (dissoc attrs :type) :packer t :unpacker t) attrs)]
        (assoc spec (keyword key) attrs)))
    {}
    fields))

(defn- extract-defaults [field-specs]
  (reduce
    (fn [map [field spec]]
      (if-let [default (:default spec)]
        (assoc map field default)
        map))
    {}
    field-specs))

(defn construct-entity-record [kind & args]
  (let [spec (get @*entity-specs* kind)
        args (->options args)
        extras (apply dissoc args (keys spec))
        record ((:*ctor* spec) nil)]
    (after-create
      (merge
        (reduce
          (fn [record [key attrs]] (assoc record key (or (key args) (:default attrs))))
          record
          (dissoc spec :*ctor*))
        extras))))

(defmacro defentity [class-sym & fields]
  (let [field-map (map-fields fields)
        kind (spear-case (name class-sym))]
    `(do
      (defrecord ~class-sym [~'kind ~'key])
      (dosync (alter *entity-specs* assoc ~kind (assoc ~field-map :*ctor* (fn [key#] (new ~class-sym ~kind key#)))))
      (defn ~(symbol kind) [& args#] (apply construct-entity-record ~kind args#))
      (extend-type ~class-sym EntityRecord (~'->native [this#] (specced-entity->native this#)))
      (defmethod native->entity ~kind [entity#] (native->specced-entity entity#)))))


