(ns hyperion.dev.spec-helper
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [*ds*]]
            [hyperion.memory :refer [new-memory-datastore]]))

(defn with-memory-datastore []
  (around [it]
    (binding [*ds* (new-memory-datastore)]
      (it))))
