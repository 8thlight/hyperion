(ns hyperion.dev.spec
  (:require [speclj.core :refer :all ]
            [hyperion.dev.spec.saving :refer [it-saves]]
            [hyperion.dev.spec.deleting :refer [it-deletes-by-key it-deletes-by-kind]]
            [hyperion.dev.spec.searching :refer [it-finds-all-kinds it-finds-by-kind it-searches-multiple-kinds it-counts-by-kind it-finds-by-key]]
            [hyperion.dev.spec.foreign-keys :refer [it-handles-foreign-keys]]
            [hyperion.dev.spec.types :refer [it-handles-types]]
            ))

(defn it-behaves-like-a-datastore []
  (list
    (context "save"
      (it-saves))
    (context "find-by-kind"
      (it-finds-by-kind))
    (context "find-by-key"
      (it-finds-by-key))
    (context "find-all-kinds"
      (it-finds-all-kinds))
    (context "searching multiple kinds"
      (it-searches-multiple-kinds))
    (context "count-by-kind"
      (it-counts-by-kind))
    (context "foreign keys"
      (tags :foreign-keys)
      (it-handles-foreign-keys))
    (context "delete-by-key"
      (it-deletes-by-key))
    (context "delete-by-kind"
      (it-deletes-by-kind))
    (context "types"
      (it-handles-types))

    ))

(run-specs)
