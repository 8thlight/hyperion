(ns hyperion.types
  (:use
    [hyperion.api :refer [pack unpack ds]]
    [hyperion.abstr :refer [->kind ds-pack-key ds-unpack-key]]))

(def #^{:dynamic true
        :doc "Map of specs decalred using defentity"} *foreign-keys* (ref []))

(defn foreign-key [kind]
  (let [kind (->kind kind)
        kind-dispatch-value (keyword (str kind "-key"))]
    (dosync
      (if (some #(= kind-dispatch-value %) @*foreign-keys*)
        kind-dispatch-value
        (do
          (alter *foreign-keys* conj kind-dispatch-value)
          (defmethod pack kind-dispatch-value [_ value] (when value (ds-pack-key (ds) value)))
          (defmethod unpack kind-dispatch-value [_ value] (when value (ds-unpack-key (ds) kind value)))
          kind-dispatch-value)))))
