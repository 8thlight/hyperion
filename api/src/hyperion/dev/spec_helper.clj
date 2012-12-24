(ns hyperion.dev.spec-helper
  (:require [speclj.core :refer :all]
            [speclj.util :refer [endl]]
            [hyperion.api :refer [*ds*]]
            [hyperion.memory :refer [new-memory-datastore]])
  (:import  [speclj SpecFailure]))

(defn with-memory-datastore []
  (around [it]
    (binding [*ds* (new-memory-datastore)]
      (it))))
