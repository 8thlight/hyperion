(ns hyperion.dev.spec-helper
  (:use
    [speclj.core]
    [hyperion.api :only [*ds*]]
    [hyperion.dev.memory :only [new-memory-datastore]]))

(defn with-memory-datastore []
  (around [it]
    (binding [*ds* (new-memory-datastore)]
      (it))))
