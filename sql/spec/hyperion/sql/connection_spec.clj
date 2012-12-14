(ns hyperion.sql.connection-spec
  (:require [speclj.core :refer :all]
            [hyperion.sql :refer :all]
            [hyperion.sql.connection :refer :all]))

(clojure.lang.RT/loadClassForName "org.sqlite.JDBC")

(describe "Connection Management"
  (with connection-url "jdbc:sqlite::memory:")
  (with connection-url-file "jdbc:sqlite:hyperion_clojure.sqlite")

  (it "binds a connection"
    (let [was-closed (atom false)]
      (with-connection @connection-url
        (reset! was-closed (.isClosed (connection))))
      (should-not @was-closed)))

  (it "uses the same connection when bound"
    (let [first-connection (atom nil)
          second-connection (atom nil)]
      (with-connection @connection-url
        (reset! first-connection (connection))
        (with-connection @connection-url
          (reset! second-connection (connection))))
      (should= @first-connection @second-connection)))

  (it "returns the result"
    (should= 1 (with-connection @connection-url 1)))

  (it "connects to multiple urls"
    (let [first-valid (atom false)
          second-valid (atom false)]
      (with-connection @connection-url
        (reset! first-valid (not (.isClosed (connection)))))
      (with-connection @connection-url-file
        (reset! second-valid (not (.isClosed (connection)))))
      (should @first-valid)
      (should @second-valid)))

  )
