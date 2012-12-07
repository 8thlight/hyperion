(ns hyperion.middleware
  (:require [hyperion.api :refer [*ds*]]))

(defn with-datastore [handler ds]
  (fn [request]
    (binding [*ds* ds]
      (handler request))))
