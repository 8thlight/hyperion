(ns hyperion.sql.connection)

; turn off C3P0 logs
(System/setProperty "com.mchange.v2.log.MLog" "com.mchange.v2.log.FallbackMLog")
(System/setProperty "com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL" "WARNING")

(import 'com.mchange.v2.c3p0.ComboPooledDataSource)

(declare #^{:dynamic true} *conn*)

(defn connected? []
  (and (bound? #'*conn*) *conn*))

(defn connection []
  (if (connected?)
    *conn*
    (throw (NullPointerException. "No current connection."))))

(def connection-pools (atom {}))

(defn- build-connection-pool [url]
  (doto (ComboPooledDataSource.)
    (.setJdbcUrl url)))

(defn- save-pool [url pool]
  (swap! connection-pools (fn [pools] (assoc pools url pool))))

(defn- connection-pool-for [url]
  (get @connection-pools url))

(defn- get-connection-pool [url]
  (if-let [pool (connection-pool-for url)]
    pool
    (let [pool (build-connection-pool url)]
      (save-pool url pool)
      pool)))

(defn- get-connection [pool url]
  (if (connected?)
    *conn*
    (.getConnection pool)))

(defn with-connection-fn [url f]
  (if (connected?)
    (f)
    (let [pool (get-connection-pool url)]
      (binding [*conn* (get-connection pool url)]
        (let [result (f)]
          (.close *conn*)
          result)))))

(defmacro with-connection [url & body]
  `(with-connection-fn ~url (fn [] ~@body)))
