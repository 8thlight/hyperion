(ns hyperion.dev.spec
  (:require [speclj.core :refer :all ]
            [hyperion.dev.spec.saving :refer [it-saves]]
            [hyperion.dev.spec.deleting :refer [it-deletes-by-key it-deletes-by-kind]]
            [hyperion.dev.spec.searching :refer [it-finds-all-kinds it-finds-by-kind it-searches-multiple-kinds it-counts-by-kind it-finds-by-key]]
            [hyperion.dev.spec.foreign-keys :refer [it-handles-foreign-keys]]
            [hyperion.dev.spec.types :refer [it-handles-types]]
            [hyperion.log :as log]))

;(log/debug!)

(defn it-behaves-like-a-datastore []
  (list
    (context "save"
      (tags :save)
      (it-saves))
    (context "find-by-kind"
      (tags :find)
      (it-finds-by-kind))
    (context "find-by-key"
      (tags :find)
      (it-finds-by-key))
    (context "find-all-kinds"
      (tags :find)
      (it-finds-all-kinds))
    (context "searching multiple kinds"
      (tags :find)
      (it-searches-multiple-kinds))
    (context "count-by-kind"
      (tags :count)
      (it-counts-by-kind))
    (context "foreign keys"
      (tags :foreign-keys)
      (it-handles-foreign-keys))
    (context "delete-by-key"
      (tags :delete)
      (it-deletes-by-key))
    (context "delete-by-kind"
      (tags :delete)
      (it-deletes-by-kind))
    (context "types"
      (tags :types)
      (it-handles-types))

    ))

(run-specs)
