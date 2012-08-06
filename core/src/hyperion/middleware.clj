(ns hyperion.middleware
  (:use
    [hyperion.core :only [*ds*]]))

(defn with-datastore [handler ds]
  (fn [request]
    (binding [*ds* ds]
      (handler request))))
