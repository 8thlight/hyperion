(ns hyperion.log
  (:import [java.util.logging Level Formatter]))

(defprotocol Logger
  (log! [this level message])
  (set-level [this level]))

(extend-protocol Logger
  java.util.logging.Logger
  (log! [this level message] (.log this level message))
  (set-level [this level] (.setLevel this level)))

(def ^{:dynamic true :tag hyperion.log.Logger} *logger*
  (doto
    (java.util.logging.Logger/getLogger "hyperion")
    (set-level Level/WARNING)))

(def DEBUG (proxy [Level] ["DEBUG" (- (.intValue Level/CONFIG) 50)]))
(def ERROR (proxy [Level] ["ERROR" (.intValue Level/SEVERE)]))
(def FORMATTER (proxy [Formatter] []
                 (format [this record] "foo")))
;                   (clojure.core/format "%1$7s %2$tH:%2$tM:%2$tS:%2$tL %3$s: %4$s\n"
;                     (.getLevel record)
;                     (.getMillis record)
;                     (.getLoggerName record)
;                     (.getMessage record)))))


;private static class LimelightFormatter extends Formatter
;  {
;    @Override
;    public String format(LogRecord logRecord)
;    {
;      return String.format("%1$7s %2$tH:%2$tM:%2$tS:%2$tL %3$s: %4$s\n",
;        logRecord.getLevel(),
;        logRecord.getMillis(),
;        logRecord.getLoggerName(),
;        logRecord.getMessage());
;    }
;  }

(defn- logify [args]
  (binding [*print-readably* nil]
    (apply pr-str args)))

(defn info [& args]
  (log! *logger* Level/INFO (logify args)))

(defn warn [& args]
  (log! *logger* Level/WARNING (logify args)))

(defn debug [& args]
  (log! *logger* DEBUG (logify args)))

(defn error [& args]
  (log! *logger* Level/SEVERE (logify args)))

(defn warn-on! []
  (set-level *logger* Level/WARNING))

(defn info-on! []
  (set-level *logger* Level/INFO))

(defn debug-on! []
  (set-level *logger* DEBUG))

(defn off! []
  (set-level *logger* Level/OFF))

(defn remove-all-handlers! []
  (doseq [handler (.getHandlers *logger*)]
    (.removeHandler *logger* handler)))

(defn add-handler! [handler]
  (.addHandler *logger* handler))