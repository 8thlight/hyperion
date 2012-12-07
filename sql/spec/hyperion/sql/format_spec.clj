(ns hyperion.sql.format-spec
  (:require [speclj.core :refer :all]
            [hyperion.sql.format :refer :all]
            [hyperion.sql.key :refer [compose-key]]))

(describe "Sql Formating"
  (context "return from db"
    (it "removes ids upon return from db"
      (should= nil (get (record<-db {"thing" 1 "id" 2} :table-name 1) "id"))
      (should= nil (:id (record<-db {"thing" 1 :id 2} :table-name 1))))

    (it "applies the kind and key"
      (let [record (record<-db {:thing 1} "table" 1)]
        (should= "table" (:kind record))
        (should= (compose-key "table" 1) (:key record))))

    (it "spear cases and keywordizes column names"
      (should= :value (:thing-column (record<-db {"thing_column" :value} "table" 1))))

    )
  )
