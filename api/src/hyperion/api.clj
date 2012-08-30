(ns hyperion.api
  (:require [clojure.string :as str]
            [hyperion.abstr :refer :all]
            [hyperion.sorting :as sort]
            [hyperion.filtering :as filter]
            [chee.datetime :refer [now]]
            [chee.util :refer [->options]]))

(declare ^{:dynamic true
           :tag hyperion.abstr.Datastore
           :doc "Stores the active datastore."} *ds*)

(defn set-ds!
  "Uses alter-var-root to set *ds*. A violent, but effective way to install a datastore."
  [^hyperion.abstr.Datastore ds]
  (alter-var-root (var *ds*) (fn [_] ds)))

(defn ds
  "Returns the currently bound datastore instance"
  []
  (if (and (bound? #'*ds*) *ds*)
    *ds*
    (throw (NullPointerException. "No Datastore bound (hyperion/*ds*). Use clojure.core/binding to bind a value or hyperion.api/set-ds! to globally set it."))))

(defn new?
  "Returns true if the record is new (not saved/doesn't have a :key), false otherwise."
  [record]
  (nil? (:key record)))

(defn- if-assoc [map key value]
  (if value
    (assoc map key value)
    map))

; ----- Hooks ---------------------------------------------

(defmulti after-create
  "Hook to alter an entity immediately after being created"
  #(keyword (:kind %)))
(defmethod after-create :default [record] record)

(defmulti before-save
  "Hook to alter values immediately before being saved
  " #(keyword (:kind %)))
(defmethod before-save :default [record] record)

(defmulti after-load
  "Hook to alter values immediately after being loaded"
  #(keyword (:kind %)))
(defmethod after-load :default [record] record)

; ----- Entity Implementation -----------------------------

(defmulti pack
  "Packers may be any object and are added to defentity specs.
  When an entity is saved, values are 'packed' before getting shipped
  off to the persistence implementation.
  You may add your own packer by declare a defmethod for your type."
  (fn [type value] type))
(defmethod pack :default [type value] value)

(defmulti unpack
  "Unpackers may be any object and are added to defentity specs.
  When an entity is loaded, values are 'unpacked' from the data in the
  persistence implementation.
  You may add your own packer by declare a defmethod for your type."
  (fn [type value] type))
(defmethod unpack :default [type value] value)

(defmethod pack :key [_ value] (when value (ds-pack-key (ds) value)))
(defmethod unpack :key [_ value] (when value (ds-unpack-key (ds) value)))

(defn- apply-type-packers [options]
  (if-let [t (:type options)]
    (-> (dissoc options :type )
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
  "PRIVATE: Used by the defentity macro to create entities."
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
  "PRIVATE: Used by the defentity macro to create entities."
  ([record] (create-entity-with-defaults record null-val-fn))
  ([record val-fn]
    (create-entity record #(->> %2 (apply-default %1) (val-fn %1)))))

(defmacro defentity
  "Used to define entities. An entity is simply an encapulation of data that
  is persisted.
  The advantage of using entities are:
   - they limit the fields persisted to only what is specified in their
     definition.
   - default values can be assigned to fields
   - types, packers, and unpackers can be assigned to fields.  Packers
     allow you to manipulate a field (perhaps serialize it) before it
     is persisted.  Unpacker conversly manipulate fields when loaded.
     Packers and unpackers maybe a fn (which will be excuted) or an
     object used to pivot the pack and unpack multimethods.
     A type (object) is simply a combined packer and unpacker.
   - constructors are provided
   - they are represented by records (defrecord) instead of plain maps.
     This allows you to use extend-type on them if you choose.

   Example:

      (defentity Citizen
        [name]
        [age :packer ->int] ; ->int is a function defined in your code.
        [gender :unpacker ->string] ; ->string is a customer function too.
        [occupation :type my.ns.Occupation] ; and then we define pack/unpack for my.ns.Occupation
        [spouse-key :type :key] ; :key is a special type that pack string keys into implementation-specific keys
        [country :default \"USA\"] ; newly created records will use the default if no value is provided
        [created-at] ; populated automaticaly
        [updated-at] ; also populated automatically
        )

        (save (citizen :name \"John\" :age \"21\" :gender :male :occupation coder :spouse-key \"abc123\"))

        ;=> #<Citizen {:kind \"citizen\" :key \"some generated key\" :country \"USA\" :created-at #<java.util.Date just-now> :updated-at #<java.util.Date just-now> ...)
  "
  [class-sym & fields]
  (let [field-map (map-fields fields)
        kind (->kind class-sym)]
    `(do
       (dosync (alter *entity-specs* assoc ~(keyword kind) ~field-map))
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
  (if (and (or (contains? spec :created-at ) (contains? record :created-at )) (= nil (:created-at record)))
    (assoc record :created-at (now))
    record))

(defn- with-updated-at [record spec]
  (if (or (contains? spec :updated-at ) (contains? record :updated-at ))
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

; ----- API -----------------------------------------------

(defn save
  "Saves a record. Any additional parameters will get merged onto the record
  before it is saved.

    (save {:kind :foo})
    ;=> {:kind \"foo\" :key \"generated key\"}
    (save {:kind :foo} {:value :bar})
    ;=> {:kind \"foo\" :value :bar :key \"generated key\"}
    (save {:kind :foo} :value :bar)
    ;=> {:kind \"foo\" :value :bar :key \"generated key\"}
    (save {:kind :foo} {:value :bar} :another :fizz)
    ;=> {:kind \"foo\" :value :bar :another :fizz :key \"generated key\"}
    (save (citizen) :name \"Joe\" :age 21 :country \"France\")
    ;=> #<Citizen {:kind \"citizen\" :name \"Joe\" :age 21 :country \"France\" ...}>
  "
  [record & args]
  (let [attrs (->options args)
        record (merge record attrs)
        entity (prepare-for-save record)
        saved (first (ds-save (ds) [entity]))]
    (native->entity saved)))

(defn save*
  "Saves multiple records at once."
  [& records]
  (doall (map native->entity (ds-save (ds) (map prepare-for-save records)))))

(defn- ->filter-operator [operator]
  (case (name operator)
    ("=" "eq") := ("<" "lt") :< ("<=" "lte") :<= (">" "gt") :> (">=" "gte") :>= ("!=" "not") :!= ("contains?" "contains" "in?" "in") :contains? (throw (Exception. (str "Unknown filter operator: " operator)))))

(defn- ->sort-direction [dir]
  (case (name dir)
    ("asc" "ascending") :asc ("desc" "descending") :desc (throw (Exception. (str "Unknown sort direction: " dir)))))

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

(defn find-by-key
  "Retrieves the value associated with the given key from the datastore.
  nil if it doesn't exist."
  [key]
  (native->entity
    (ds-find-by-key (ds) key)))

(defn reload
  "Returns a freshly loaded record based on the key of the given record."
  [entity]
  (find-by-key (:key entity)))

(defn find-by-kind
  "Returns all records of the specified kind that match the filters provided.

    (find-by-kind :dog) ; returns all records with :kind of \"dog\"
    (find-by-kind :dog :filters [:= :name \"Fido\"]) ; returns all dogs whos name is Fido
    (find-by-kind :dog :filters [[:> :age 2][:< :age 5]]) ; returns all dogs between the age of 2 and 5 (exclusive)
    (find-by-kind :dog :sorts [:name :asc]) ; returns all dogs in alphebetical order of their name
    (find-by-kind :dog :sorts [[:age :desc][:name :asc]]) ; returns all dogs ordered from oldest to youngest, and gos of the same age ordered by name
    (find-by-kind :dog :limit 10) ; returns upto 10 dogs in undefined order
    (find-by-kind :dog :sorts [:name :asc] :limit 10) ; returns upto the first 10 dogs in alphebetical order of their name
    (find-by-kind :dog :sorts [:name :asc] :limit 10 :offset 10) ; returns the second set of 10 dogs in alphebetical order of their name

  Filter operations and acceptable syntax:
    := \"=\" \"eq\"
    :< \"<\" \"lt\"
    :<= \"<=\" \"lte\"
    :> \">\" \"gt\"
    :>= \">=\" \"gte\"
    :!= \"!=\" \"not\"
    :contains? \"contains?\" :contains \"contains\" :in? \"in?\" :in \"in\"

  Sort orders and acceptable syntax:
    :asc \"asc\" :ascending \"ascending\"
    :desc \"desc\" :descending \"descending\"
  "
  [kind & args]
  (let [options (->options args)
        kind (name kind)]
    (find-records-by-kind kind
      (:filters options)
      (parse-sorts (:sorts options))
      (:limit options)
      (:offset options))))

(defn find-all-kinds
  "Same as find-by-kind except that it'll returns results of any kind
  WARNING: This methods is almost certainly horribly inefficient.  Use with caution."
  [& args]
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

(defn count-by-kind
  "Counts records of the specified kind that match the filters provided."
  [kind & args]
  (let [options (->options args)
        kind (name kind)]
    (count-records-by-kind kind (:filters options))))

(defn- count-records-by-all-kinds [filters]
  (let [kinds (ds-all-kinds (ds))
        results (flatten (map #(count-records-by-kind % filters) kinds))]
    (apply + results)))

(defn count-all-kinds
  "Counts records of any kind that match the filters provided."
  [& args]
  (let [options (->options args)]
    (count-records-by-all-kinds (:filters options))))

(defn delete-by-key
  "Removes the record stored with the given key.
  Returns nil no matter what."
  [key]
  (ds-delete-by-key (ds) key)
  nil)

(defn delete-by-kind
  "Deletes all records of the specified kind that match the filters provided."
  [kind & args]
  (let [options (->options args)
        kind (->kind kind)]
    (ds-delete-by-kind (ds) kind (parse-filters kind (:filters options)))
    nil))

; ----- Factory -------------------------------------------

(defn new-datastore
  "Factory methods to create datastore instances.  Just provide the
  :implementation you want (along with configuration) and we'll load
  the namespace and construct the instance for you.

    (new-datastore :implementation :memory) ; create a new in-memory datastore
    (new-datastore :implementation :sqlite :connection-url \"jdbc:sqlite:\") ; creates a new sqlite datastore
  "
  [& args]
  (let [options (->options args)]
    (if-let [implementation (:implementation options)]
      (try
        (let [ns-sym (symbol (str "hyperion." (name implementation)))]
          (require ns-sym)
          ((ns-resolve (the-ns ns-sym) (symbol (format "new-%s-datastore" (name implementation)))) options))
        (catch java.io.FileNotFoundException e
          (throw (Exception. (str "Can't find datastore implementation: " implementation) e))))
      (throw (Exception. "new-datastore requires an :implementation entry (:memory, :mysql, :mongo, ...)"))
      )))
