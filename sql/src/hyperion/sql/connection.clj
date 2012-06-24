(ns hyperion.sql.connection)

(declare #^{:dynamic true} *conn*)

(defn connection []
  (if (bound? #'*conn*)
    *conn*
    (throw (NullPointerException. "No current connection."))))

(defmacro with-connection-url [url & body]
  `(binding [*conn* (java.sql.DriverManager/getConnection ~url)]
     ~@body))

(defmacro with-connection [conn & body]
  `(binding [*conn* ~conn]
     ~@body))
