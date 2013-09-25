(ns hyperion.sql.spec-helper
  (:require [speclj.core :refer :all]
            [speclj.platform]
            [hyperion.sql.connection :refer [with-connection]]
            [hyperion.sql.jdbc :refer [rollback]]))

(defn str-contains? [s sub]
  (not= -1 (.indexOf s sub)))

(defmacro error-msg-contains? [msg form]
  `(try
     ~form
     (throw (-fail (str "Expected and exception with a message containing " ~msg " but no exception was thrown")))
     (catch Exception e#
       (let [message# (.getMessage e#)]
         (cond
          (speclj.platform/failure? e#)
            (throw e#)
          (not (str-contains? (.getMessage e#) ~msg))
            (throw (-fail (str "Expected and exception with a message containing \"" ~msg "\" but got: \"" message# "\"")))
          :else e#)))))

(defn with-rollback [url]
  (around [it]
    (with-connection url
      (rollback
        (it)))))

(defn with-connection-and-rollback [url]
  (around [it]
    (with-connection url
      (rollback
        (it)))))
