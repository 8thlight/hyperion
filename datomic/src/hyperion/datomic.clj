(ns hyperion.datomic
  (:require [hyperion.core :refer [Datastore]]
            [hyperion.key :refer (compose-key decompose-key)]
            [chee.util :refer [->options]]))

(defn- save-records [db records])

(defn- find-by-key [db key])

(defn- delete-by-key [db key])

(defn- find-by-kind [db kind filters sorts limit offset])

(defn- delete-by-kind [db kind filters])

(defn- count-by-kind [db kind filters])

(defn- list-all-kinds [db])

(deftype DatomicDatastore [db]
  Datastore
  (ds-save [this records] (save-records db records))
  (ds-delete-by-kind [this kind filters] (delete-by-kind db kind filters))
  (ds-delete-by-key [this key] (delete-by-key db key))
  (ds-count-by-kind [this kind filters] (count-by-kind db kind filters))
  (ds-find-by-key [this key] (find-by-key db key))
  (ds-find-by-kind [this kind filters sorts limit offset] (find-by-kind db kind filters sorts limit offset))
  (ds-all-kinds [this] (list-all-kinds db))
  (ds-pack-key [this value] value)
  (ds-unpack-key [this value] value))

(defn new-datomic-datastore [& args]
      (DatomicDatastore. :blah))

