(ns hyperion.core)

(def DS (atom nil))

(defprotocol Datastore
  (ds-save [this record])
  (ds-find-by-key [this key])
  (ds-delete [this key])
  (ds-find-by-kind [this kind filters sorts])
  )

(defn- no-ds-installed! [] (throw (NullPointerException. "No Datastore (hyperion/DS) installed.")))
(extend-type nil
  Datastore
  (ds-save [_ _] (no-ds-installed!))
  (ds-delete [_ _] (no-ds-installed!))
  (ds-find-by-key [_ _] (no-ds-installed!))
  (ds-find-by-kind [_ _ _ _] (no-ds-installed!))
  )

(defn new? [record]
  (nil? (:key record)))

(defn ->key [thing]
  (or (:key thing) thing))

(defn save [record] (ds-save @DS record))

(defn find-by-key [key] (ds-find-by-key @DS (->key key)))

(defn delete [key] (ds-delete @DS (->key key)))

(defn find-by-kind [kind]
  (ds-find-by-kind @DS kind [] []))


