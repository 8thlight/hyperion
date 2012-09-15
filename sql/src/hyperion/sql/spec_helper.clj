(ns hyperion.sql.spec-helper
  (:use
    [speclj.core]
    [hyperion.sql.connection :only [with-connection-url]]
    [hyperion.sql.jdbc :only [rollback]])
  (:import [speclj SpecFailure]))

(defn str-contains? [s sub]
  (not= -1 (.indexOf s sub)))

(defmacro error-msg-contains? [msg form]
  `(try
     ~form
     (throw (SpecFailure. (str "Expected and exception with a message containing " ~msg " but no exception was thrown")))
     (catch Exception e#
       (let [message# (.getMessage e#)]
         (cond
          (.isInstance SpecFailure e#)
            (throw e#)
          (not (str-contains? (.getMessage e#) ~msg))
            (throw (SpecFailure. (str "Expected and exception with a message containing \"" ~msg "\" but got: \"" message# "\"")))
          :else e#)))))

(defn with-rollback [url]
  (around [it]
    (with-connection-url url
      (rollback
        (it)))))

(defn with-connection-and-rollback [url]
  (around [it]
    (with-connection-url url
      (rollback
        (it)))))
