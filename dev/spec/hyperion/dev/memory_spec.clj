(ns hyperion.dev.memory-spec
  (:require
    [speclj.core :refer :all]
    [hyperion.core :refer [*ds*]]
    [hyperion.memory :refer [new-memory-datastore]]
    [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]))

(describe "In Memory datastore"
  (around [it]
    (binding [*ds* (new-memory-datastore)]
      (it)))
  (it-behaves-like-a-datastore))
