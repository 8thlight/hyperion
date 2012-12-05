(ns hyperion.abstr
  (:require [chee.string :refer [gsub spear-case]]))

(defprotocol Datastore
  "Protocol for Hyperion implementations."
  (ds-save [this records])
  (ds-delete-by-key [this key])
  (ds-delete-by-kind [this kind filters])
  (ds-count-by-kind [this kind filters])
  (ds-find-by-key [this key])
  (ds-find-by-kind [this kind filters sorts limit offset])
  (ds-all-kinds [this])
  (ds-pack-key [this value])
  (ds-unpack-key [this kind value]))

(def #^{:dynamic true
        :doc "Map of specs decalred using defentity"} *entity-specs* (ref {}))

(defprotocol AsKind
  "Protocol to coerce values into a 'kind' string."
  (^{:doc "Coerces value into a 'kind' string"} ->kind [this]))

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
  (->kind [this] nil))

(defprotocol AsField
  "Protocol to coerce values into a field name"
  (^{:doc "Coerces value into a field name"} ->field [this]))

(extend-protocol AsField
  java.lang.String
  (->field [this] (keyword this))

  clojure.lang.Keyword
  (->field [this] this)

  clojure.lang.Symbol
  (->field [this] (keyword this)))

(defprotocol Specable
  "Protocol to retrieve an entity-spec as defined in defentity."
  (^{:doc "Retrieves the entity-spec for the value"} spec-for [this]))

(extend-protocol Specable
  clojure.lang.IPersistentMap
  (spec-for [this] (spec-for (->kind this)))

  clojure.lang.Keyword
  (spec-for [this] (get @*entity-specs* this))

  java.lang.String
  (spec-for [this] (spec-for (keyword this)))

  nil
  (spec-for [this] nil))
