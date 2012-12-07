(ns hyperion.middleware-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [*ds*]]
            [hyperion.fake :refer [new-fake-datastore]]
            [hyperion.middleware :refer [with-datastore]]))

(describe "Hyperion Ring Middleware"
  (it "binds a ds"
    (let [ds (new-fake-datastore)
          req {:thing 2}]
      (letfn [(fake-handler [request]
                (should= ds *ds*)
                (should= req request))]
        ((with-datastore fake-handler ds) req)))))
