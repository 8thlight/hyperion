(ns hyperion.core
  (:require
    [clojure.string :as str]))

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
  (ds->kind [this thing])
  (ds->ds-key [this thing])
  (ds->string-key [this thing])
  (ds-save [this record])
  (ds-save* [this records])
  (ds-delete [this keys])
  (ds-count-by-kind [this kind filters])
  (ds-count-all-kinds [this filters])
  (ds-find-by-key [this key])
  (ds-find-by-kind [this kind filters sorts limit offset])
  (ds-find-all-kinds [this filters sorts limit offset])
  (ds-entity->map [this entity])
  (ds-map->entity [this record]))

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

(def #^{:dynamic true} *entities* (ref {}))

(defmulti entity->record kind)

(defmethod entity->record nil [entity]
  nil)

(defn known-entity->record
  ([entity]
    (let [kind (->kind entity)
          spec (get @*entities* kind)]
      (known-entity->record entity kind spec)))
  ([entity kind spec]
    (let [key (ds->string-key (ds) entity)
          record ((:*ctor* spec) key)]
      (after-load
        (reduce
          (fn [record [field value]]
            (let [field (keyword field)]
              (assoc record field (unpack-field (:unpacker (field spec)) value))))
          record
          (ds-entity->map (ds) entity))))))

(defn- unknown-entity->record [entity kind]
  (after-load
    (reduce
      (fn [record entry] (assoc record (keyword (key entry)) (val entry)))
      {:kind kind :key (ds->string-key (ds) entity)}
      (ds-entity->map (ds) entity))))

(defmethod entity->record :default [entity]
  (let [kind (.getKind entity)
        spec (get @*entities* kind)]
    (if spec
      (known-entity->record entity kind spec)
      (unknown-entity->record entity kind))))

(defprotocol EntityRecord
  (->entity [this]))

(defn- unknown-record->entity [record kind]
  (ds-map->entity (ds) record))

(defn known-record->entity
  ([record]
    (let [kind (:kind record)
          spec (get @*entities* kind)]
      (known-record->entity record kind spec)))
  ([record kind spec]
    (ds-map->entity (ds)
      (reduce
        (fn [marshaled [field attrs]]
          (assoc marshaled field (pack-field (:packer (field spec)) (field record))))
        {}
        (dissoc spec :*ctor*)))))

(extend-type clojure.lang.APersistentMap
  EntityRecord
  (->entity [record]
    (let [kind (:kind record)
          spec (get @*entities* kind)]
      (if spec
        (known-record->entity record kind spec)
        (unknown-record->entity record kind)))))

; ----- Raw CRUD API --------------------------------------

(defn save [record & args]
  (let [attrs (->options args)
        record (merge record attrs)
;        record (with-updated-timestamps record)
        record (before-save record)
        entity (->entity record)]
    (ds-save (ds) entity)))

(defn save* [records]
  (ds-save* (ds) records))

(defn find-by-key [key]
  (ds-find-by-key (ds) (->key key)))

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

; ----- String Util ---------------------------------------

(defn gsub
  "Matches patterns and replaces those matches with a specified value.
  Expects a string to run the operation on, a pattern in the form of a
  regular expression, and a function that handles the replacing."
  [value pattern sub-fn]
  (loop [matcher (re-matcher pattern value) result [] last-end 0]
    (if (.find matcher)
      (recur matcher
          (conj result
            (.substring value last-end (.start matcher))
            (sub-fn (re-groups matcher)))
          (.end matcher))
      (apply str (conj result (.substring value last-end))))))

(defn spear-case [value]
  (str/lower-case
    (gsub
      (str/replace (name value) "_" "-")
      #"([a-z])([A-Z])" (fn [[_ lower upper]] (str lower "-" upper)))))

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
  (let [spec (get @*entities* kind)
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
        kind (spear-case class-sym)]
    `(do
       (defrecord ~class-sym [~'kind ~'key])
       (dosync (alter *entities* assoc ~kind (assoc ~field-map :*ctor* (fn [key#] (new ~class-sym ~kind key#)))))
       (defn ~(symbol kind) [& args#] (apply construct-entity-record ~kind args#))
       (extend-type ~class-sym EntityRecord (~'->entity [this#] (known-record->entity this#)))
       (defmethod entity->record ~kind [entity#] (known-entity->record entity#)))))


