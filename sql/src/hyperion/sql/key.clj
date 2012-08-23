(ns hyperion.sql.key
  (:require [hyperion.key :as base]
            [clojure.string :as string]))

(defn compose-key [^String kind id]
  (when (or (nil? id) (not (integer? id)))
    (throw (Exception. (str "id must be integer to create key. was: " id))))
  (base/compose-key kind (str id)))

(defn decompose-key [^String key]
  (let [[kind id-str] (base/decompose-key key)]
    (list kind (Long/parseLong id-str))))
