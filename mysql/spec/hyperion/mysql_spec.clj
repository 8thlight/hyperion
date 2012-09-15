(ns hyperion.mysql-spec
  (:use [speclj.core]
        [hyperion.sql.spec-helper]
        [hyperion.dev.spec :only [it-behaves-like-a-datastore]]
        [hyperion.api :only [*ds* new-datastore save find-by-key]]
        [hyperion.sql.jdbc :only [execute-mutation]]
        [hyperion.sql.query]
        [hyperion.mysql :only [new-mysql-datastore]]))

(def create-table-query
  "CREATE TABLE %s (
    id INTEGER NOT NULL AUTO_INCREMENT,
    name VARCHAR(35),
    first_name VARCHAR(35),
    inti INTEGER,
    data VARCHAR(32),
    PRIMARY KEY (id)
  )")

(defn create-table [table-name]
  (execute-mutation
    (make-query (format create-table-query table-name))))

(def drop-table-query "DROP TABLE IF EXISTS %s")

(defn drop-table [table-name]
  (execute-mutation
    (make-query (format drop-table-query table-name))))

(describe "MySQL Datastore"

  (context "creation"

    (it "with a kv pairs as params"
      (let [ds (new-mysql-datastore :connection-url "jdbc:mysql://localhost:3306/hyperion?user=root" :database "hyperion")]
        (should= "hyperion" (.database (.db ds)))))

    (it "with factory fn"
      (let [ds (new-datastore :implementation :mysql :connection-url "jdbc:mysql://localhost:3306/hyperion?user=root" :database "hyperion")]
        (should= "hyperion" (.database (.db ds)))))

    )

  (context "live"
    (with-connection-and-rollback "jdbc:mysql://localhost:3306/hyperion?user=root")

    (around [it]
      (try
        (create-table "testing")
        (create-table "other_testing")
        (binding [*ds* (new-mysql-datastore :connection-url "jdbc:mysql://localhost:3306/hyperion?user=root" :database "hyperion")]
          (it))
        (finally
          (drop-table "testing")
          (drop-table "other_testing")
          )))

    (it-behaves-like-a-datastore)

    (context "SQL Injection"
      (it "sanitizes strings to be inserted"
        (let [evil-string "my evil string' --"
              record (save {:kind :testing :name evil-string})]
          (should= evil-string (:name (find-by-key (:key record))))))

      (it "sanitizes table names"
        (error-msg-contains?
          "Table 'hyperion.my_evil_name`___' doesn't exist"
          (save {:kind "my-evil-name` --" :name "test"})))

      (it "sanitizes column names"
        (error-msg-contains?
          "Unknown column 'my_evil_name`___' in 'field list'"
          (save {:kind :testing (keyword "my-evil-name` --") "test"}))))
    )
  )
