(ns hyperion.data-store-spec
  (:use [speclj.core]
        [hyperion.data-store]
        [hyperion.data-store.in-memory :only [new-in-memory-persistor]]))

(describe "data store"
  (with in-memory-persistor (new-in-memory-persistor))
  (with record1 {:name "Jimmy John's" :address "1234 Smart way"})
  (with record2 {:name "Randy Savage" :address "Yo Mama's House"})
  (with collection-name "CollectionName")

  (context "find and create"
    (it "remembers a record"
      (let [created-record (.create @in-memory-persistor @collection-name @record1)]
        (should= [created-record] (.find-where @in-memory-persistor @collection-name {:name (:name @record1)}))))

    (it "remembers two records"
      (let [created-record1 (.create @in-memory-persistor @collection-name @record1)
            created-record2 (.create @in-memory-persistor @collection-name @record2)]
        (should= [created-record1] (.find-where @in-memory-persistor @collection-name {:address (:address @record1)}))
        (should= [created-record2] (.find-where @in-memory-persistor @collection-name {:address (:address @record2)}))))

    (it "incriments the id"
      (let [created-record1 (.create @in-memory-persistor @collection-name @record1)
            created-record2 (.create @in-memory-persistor @collection-name @record2)]
        (.delete @in-memory-persistor @collection-name created-record2)
        (should= 3 (:id (.create @in-memory-persistor @collection-name @record1)))))
    )

  (context "delete"

    (it "deletes one record"
      (let [created-record (.create @in-memory-persistor @collection-name @record1)]
        (.delete @in-memory-persistor @collection-name created-record)
        (should= [] (.find-where @in-memory-persistor @collection-name {}))))

    (it "deletes multiple records"
      (let [created-record1 (.create @in-memory-persistor @collection-name @record1)
            created-record2 (.create @in-memory-persistor @collection-name @record2)]
        (.delete @in-memory-persistor @collection-name created-record1)
        (.delete @in-memory-persistor @collection-name created-record2)
        (should= [] (.find-where @in-memory-persistor @collection-name {}))))

    (it "does not throw if delete is called before a record of the same type is created"
      (should-not-throw (.delete @in-memory-persistor @collection-name @record1)))

    )

  (context "update"
    (it "updates a record"
      (let [created-record (.create @in-memory-persistor @collection-name @record1)
            updated-record (merge created-record {:address "123 WCW Lane"})]
        (.update @in-memory-persistor @collection-name updated-record)
        (should= [updated-record] (.find-where @in-memory-persistor @collection-name {}))))

    (it "returns the updated record if the update was valid"
      (let [created-record (.create @in-memory-persistor @collection-name @record1)
            updated-record (merge created-record {:address "123 WCW Lane"})]
        (should= updated-record (.update @in-memory-persistor @collection-name updated-record))))

    (it "does nothing when record doesn't exist"
      (.update @in-memory-persistor @collection-name (merge @record2 {:address "123 WCW Lane"}))
      (should= [] (.find-where @in-memory-persistor @collection-name {})))
    )
  )
