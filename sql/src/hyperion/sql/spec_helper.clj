(ns hyperion.sql.spec-helper
  (:use
    [speclj.core]
    [hyperion.sql.connection :only [with-connection-url]]
    [hyperion.sql.jdbc]))

(defn with-connection-and-rollback [url]
  (around [it]
    (with-connection-url url
      (rollback
        (it)))))
