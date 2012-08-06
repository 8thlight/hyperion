(ns hyperion.middleware-spec
  (:use
    [speclj.core]
    [hyperion.core :only [*ds*]]
    [hyperion.middleware :only [with-datastore]]))

(describe "Hyperion Ring Middleware"
  (it "binds a ds"
    (let [ds {:thing 1}
          req {:thing 2}]
      (letfn [(fake-handler [request]
                (should= ds *ds*)
                (should= req request))]
        ((with-datastore fake-handler ds) req)))))
