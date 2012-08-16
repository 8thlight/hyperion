(ns hyperion.memory-spec
  (:use
    [speclj.core]
    [hyperion.core :only [*ds*]]
    [hyperion.memory :only [new-memory-datastore]]
    [hyperion.dev.spec :only [it-behaves-like-a-datastore]]))

(describe "In Memory datastore"
  (around [it]
    (binding [*ds* (new-memory-datastore)]
      (it)))
  (it-behaves-like-a-datastore))
