(ns hyperion.middleware
  (:use
    [hyperion.api :only [*ds*]]))

(defn with-datastore [handler ds]
  (fn [request]
    (binding [*ds* ds]
      (handler request))))
