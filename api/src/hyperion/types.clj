(ns hyperion.types
  (:require [hyperion.api :refer [ds pack unpack]]
            [hyperion.abstr :refer [->kind ds-pack-key ds-unpack-key]]))

(defn foreign-key
  "Used as a :type when defining an entity.

    [spouse-key :type (foreign-key :citizen)]"
  [kind]
  (let [kind (->kind kind)
        kind-dispatch-value (keyword (str "-hyperion-" kind "-key"))]
    (when (= (.getMethod pack kind-dispatch-value) (.getMethod pack :default))
      (defmethod pack kind-dispatch-value [_ value] (when value (ds-pack-key (ds) value)))
      (defmethod unpack kind-dispatch-value [_ value] (when value (ds-unpack-key (ds) kind value))))
    kind-dispatch-value))