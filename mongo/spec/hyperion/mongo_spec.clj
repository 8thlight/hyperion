(ns hyperion.mongo-spec
  (:require [speclj.core :refer :all ]
            [hyperion.api :refer :all ]
            [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]
            [hyperion.mongo.spec-helper :refer [with-testable-mongo-datastore]]
            [hyperion.mongo :refer :all ]))

(describe "Mongo Datastore"

  (context "Client connection"

    (it "can open client with one address"
      (with-open [mongo (open-mongo :host "127.0.0.1" :port 27017)]
        (should= com.mongodb.Mongo (class mongo))
        (should= [["127.0.0.1" 27017]] (map address->seq (.getAllAddress mongo)))))

    (it "can open client with one address and default port"
      (with-open [mongo (open-mongo :host "127.0.0.1")]
        (should= com.mongodb.Mongo (class mongo))
        (should= [["127.0.0.1" 27017]] (map address->seq (.getAllAddress mongo)))))

    (it "can open client with multiple addresses"
      (with-open [mongo (open-mongo :servers [["127.0.0.1" 27017] ["foo.com" 27018]])]
        (should= com.mongodb.Mongo (class mongo))
        (should= [["127.0.0.1" 27017] ["foo.com" 27018]] (map address->seq (.getAllAddress mongo)))))

    (it "opens a database from mongo"
      (with-open [mongo (open-mongo :host "127.0.0.1" :port 27017)]
        (let [db (open-database mongo "foo")]
          (should (.isInstance com.mongodb.DB db))
          (should= "foo" (.getName db))
          (should= com.mongodb.WriteConcern/SAFE (.getWriteConcern db)))))

    (it "opens a database from mongo with credentials"
      (with-open [mongo (open-mongo :host "127.0.0.1" :port 27017)]
        (let [db (open-database mongo "foo" :username "joe" :password "blow")]
          (should= "foo" (.getName db))
          (should= false (.isAuthenticated db)) ; no real way to test that the credentials are used
          )))

    (it "uses the specified write concern for the database"
      (with-open [mongo (open-mongo :host "127.0.0.1" :port 27017)]
        (should= com.mongodb.WriteConcern/FSYNC_SAFE (.getWriteConcern (open-database mongo "foo" :write-concern :fsync-safe )))
        (should= com.mongodb.WriteConcern/JOURNAL_SAFE (.getWriteConcern (open-database mongo "foo" :write-concern :journal-safe )))
        (should= com.mongodb.WriteConcern/MAJORITY (.getWriteConcern (open-database mongo "foo" :write-concern :majority )))
        (should= com.mongodb.WriteConcern/NONE (.getWriteConcern (open-database mongo "foo" :write-concern :none )))
        (should= com.mongodb.WriteConcern/NORMAL (.getWriteConcern (open-database mongo "foo" :write-concern :normal )))
        (should= com.mongodb.WriteConcern/REPLICAS_SAFE (.getWriteConcern (open-database mongo "foo" :write-concern :replicas-safe )))
        (should= com.mongodb.WriteConcern/SAFE (.getWriteConcern (open-database mongo "foo" :write-concern :safe )))
        (should-throw Exception "Unknown write-concern: :blah" (open-database mongo "foo" :write-concern :blah ))))
    )

  (context "Creating mongo datastore"

    (it "from db"
      (with-open [mongo (open-mongo :host "127.0.0.1" :port 27017)]
        (let [db (open-database mongo "foo")
              ds (new-mongo-datastore db)]
          (should= hyperion.mongo.MongoDatastore (class ds))
          (should= db (.db ds))
          (should= "foo" (.getName (.db ds))))))

    (it "from options"
      (let [ds (new-mongo-datastore :host "127.0.0.1" :port 27017 :database "bar")]
        (try
          (let [db (.db ds)
                mongo (.getMongo db)]
            (should= hyperion.mongo.MongoDatastore (class ds))
            (should= db (.db ds))
            (should= [["127.0.0.1" 27017]] (map address->seq (.getAllAddress mongo)))
            (should= "bar" (.getName db)))
          (finally (.close (.getMongo (.db ds)))))))

    (it "using factory"
      (let [ds (new-datastore :implementation :mongo :host "127.0.0.1" :port 27017 :database "bar")]
        (try
          (let [db (.db ds)
                mongo (.getMongo db)]
            (should= hyperion.mongo.MongoDatastore (class ds))
            (should= db (.db ds))
            (should= [["127.0.0.1" 27017]] (map address->seq (.getAllAddress mongo)))
            (should= "bar" (.getName db)))
          (finally (.close (.getMongo (.db ds)))))))
  )

  (context "Live"
    (with-testable-mongo-datastore)

    (it-behaves-like-a-datastore)

    (it "can store a sequence of ints"
      (let [saved (save {:kind "triplet" :values [1 2 3]})]
        (should= [1 2 3] (:values saved))
        (should= [1 2 3] (:values (find-by-key (:key saved))))))

    (it "can store a sequence of strings"
      (let [saved (save {:kind "triplet" :values ["1" "2" "3"]})]
        (should= ["1" "2" "3"] (:values saved))
        (should= ["1" "2" "3"] (:values (find-by-key (:key saved))))))

    )
)



