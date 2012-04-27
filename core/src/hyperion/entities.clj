(ns hyperion.entities)

(defmulti pack (fn [packer value] packer))
(defmethod pack :default [packer value] value)

(def *entities* (ref {}))

(defprotocol Packed
  (unpack [this]))

(defn kind [thing]
  (cond
;    (isa? (class thing) Entity) (.getKind thing)
    (map? thing) (:kind thing)
    :else nil))

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

(defmulti entity->record kind)

(defmethod entity->record nil [entity]
  nil)

(defn known-entity->record
  ([entity]
    (let [kind (.getKind entity)
          spec (get @*entities* kind)]
      (known-entity->record entity kind spec)))
  ([entity kind spec]
    (let [key (key->string (.getKey entity))
          record ((:*ctor* spec) key)]
      (after-load
        (reduce
          (fn [record [field value]]
            (let [field (keyword field)]
              (assoc record field (unpack-field (:unpacker (field spec)) value))))
          record
          (.getProperties entity))))))

(defn- unknown-entity->record [entity kind]
  (after-load
    (reduce
      (fn [record entry] (assoc record (keyword (key entry)) (val entry)))
      {:kind kind :key (key->string (.getKey entity))}
      (.getProperties entity))))

(defmethod entity->record :default [entity]
  (let [kind (.getKind entity)
        spec (get @*entities* kind)]
    (if spec
      (known-entity->record entity kind spec)
      (unknown-entity->record entity kind))))

(defprotocol EntityRecord
  (->entity [this]))

(defn- unknown-record->entity [record kind]
  (let [key (string->key (:key record))
        entity (if key (Entity. key) (Entity. kind))]
    (doseq [[field value] (dissoc record :kind :key)]
      (.setProperty entity (name field) value))
    entity))

(defn known-record->entity
  ([record]
    (let [kind (:kind record)
          spec (get @*entities* kind)]
      (known-record->entity record kind spec)))
  ([record kind spec]
    (let [key (string->key (:key record))
          entity (if key (Entity. key) (Entity. kind))]
      (doseq [[field attrs] (dissoc spec :*ctor*)]
        (.setProperty entity (name field) (pack-field (:packer (field spec)) (field record))))
      entity)))

(extend-type clojure.lang.APersistentMap
  EntityRecord
  (->entity [record]
    (let [kind (:kind record)
          spec (get @*entities* kind)]
      (if spec
        (known-record->entity record kind spec)
        (unknown-record->entity record kind)))))

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

