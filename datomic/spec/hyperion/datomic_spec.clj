(ns hyperion.datomic_spec
  (:require [speclj.core :refer :all ]
            [hyperion.core :refer :all ]
            [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]
            [hyperion.datomic.spec-helper :refer [with-testable-datomic-datastore]]
            [hyperion.datomic :refer :all ]))

(describe "Datomic Datastore"

  (context "Live"
    (with-testable-datomic-datastore)

    (it-behaves-like-a-datastore)
    )
)



