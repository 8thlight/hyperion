(ns hyperion.core)

(defn ->options
  "Takes keyword argument and converts them to a map.  If the args are prefixed with a map, the rest of the
  args are merged in."
  [options]
  (if (map? (first options))
    (merge (first options) (apply hash-map (rest options)))
    (apply hash-map options)))

(def DS (atom nil))

(defprotocol Datastore
  (ds-save [this record])
  (ds-save* [this records])
  (ds-delete [this keys])
  (ds-find-by-key [this key])
  (ds-find-by-kind [this kind filters sorts limit offset])
  )

(defn- no-ds-installed! [] (throw (NullPointerException. "No Datastore (hyperion/DS) installed.")))
(extend-type nil
  Datastore
  (ds-save [_ _] (no-ds-installed!))
  (ds-save* [_ _] (no-ds-installed!))
  (ds-delete [_ _] (no-ds-installed!))
  (ds-find-by-key [_ _] (no-ds-installed!))
  (ds-find-by-kind [_ _ _ _ _ _] (no-ds-installed!))
  )

(defn new? [record]
  (nil? (:key record)))

(defn ->key [thing]
  (or (:key thing) thing))

(defn save [record & args]
  (let [attrs (->options args)
        record (merge record attrs)]
  (when (not (:kind record)) (throw (Exception. "Can't save record without a :kind")))
  (ds-save @DS record)))

(defn save* [records]
  (ds-save* @DS records))

(defn find-by-key [key]
  (ds-find-by-key @DS (->key key)))

(defn reload [entity-or-key]
  (find-by-key entity-or-key))

(defn delete [& keys] (ds-delete @DS (map ->key keys)))

(defn find-by-kind [kind]
  (ds-find-by-kind @DS kind [] [] nil nil))


