(ns hyperion.gae.spec-helper
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [*ds*]]
            [hyperion.gae :refer [new-gae-datastore]])
  (:import
    [com.google.appengine.tools.development.testing
     LocalServiceTestConfig
     LocalDatastoreServiceTestConfig
     LocalBlobstoreServiceTestConfig
     LocalServiceTestHelper]
    [com.google.appengine.api.datastore.dev LocalDatastoreService]
    [com.google.apphosting.api ApiProxy]
    [java.util.logging Logger Level]))

(defn tear-down-local-datastore []
  (.stop (ApiProxy/getDelegate))
  (ApiProxy/clearEnvironmentForCurrentThread))

(defn set-up-local-datastore []
  (.setUp (LocalServiceTestHelper.
    (into-array LocalServiceTestConfig
      [(LocalBlobstoreServiceTestConfig.) (LocalDatastoreServiceTestConfig.)]))))

(defn with-local-datastore []
  (around [it]
    (try
      (.setLevel (Logger/getLogger (.getName LocalDatastoreService)) (Level/OFF))
      (set-up-local-datastore)
      (binding [*ds* (new-gae-datastore)]
        (it))
      (finally (tear-down-local-datastore)))))
