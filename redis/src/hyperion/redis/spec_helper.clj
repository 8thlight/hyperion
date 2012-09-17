(ns hyperion.redis.spec-helper
  (:require [speclj.core :refer :all ]
            [hyperion.redis :refer :all ]
            [taoensso.carmine :as r]
            [hyperion.api :refer [*ds*]]))

(defn- clear-db [db]
  (let [test-keys (carmine db (r/keys "*"))]
      (when (seq test-keys)
        (carmine db (apply r/del test-keys)))))

(defn with-testable-redis-datastore []
  (list
    (around [it]
      (let [ds (new-redis-datastore :host "127.0.0.1" :port 6379)]
        (binding [*ds* ds]
          (try
            (it)
            (finally
              (clear-db (.db ds)))))))))
