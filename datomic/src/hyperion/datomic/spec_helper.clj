(ns hyperion.datomic.spec-helper
  (:require [speclj.core :refer :all ]
            [hyperion.datomic :refer :all ]
            [hyperion.core :refer [*ds*]]))

(defn with-testable-datomic-datastore []
  (around [it]
    (let [ds (new-datomic-datastore :foo)]
      (binding [*ds* ds]
        (try
          (it)
          (finally
            ; clear
            ))))))



