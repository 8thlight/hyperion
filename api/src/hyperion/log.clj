(ns hyperion.log
  (:require [taoensso.timbre :as timbre]
            [clojure.string :as str]))

(def ^:private levels [:trace :debug :info :warn :error :fatal :report])

(defmacro ^:private def-logger
  [level]
  (let [level-name (name level)]
    `(do
       (defn ~(symbol (str level-name "!"))
         ~(str "Sets the log level to " (str/capitalize level-name))
         []
         (timbre/set-level! ~level))
       (defmacro ~(symbol level-name)
         ~(str "Log given arguments at " (str/capitalize level-name) " level.")
         ~'{:arglists '([message & more] [throwable message & more])}
         [& sigs#]
         `(timbre/log ~~level ~@sigs#)))))

(defmacro ^:private def-loggers
  [] `(do ~@(map (fn [level] `(def-logger ~level)) levels)))

(def-loggers) ; Actually define a logger for each logging level

(defn boot-log-level! []
  (println "booting log level: " (System/getenv "LOG_LEVEL"))
  (if-let [level (System/getenv "LOG_LEVEL")]
    (timbre/set-level! (keyword level))
    (info!)))

(boot-log-level!)

