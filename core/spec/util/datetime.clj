(ns util.datetime
  (:import
    [java.util Date Calendar GregorianCalendar]
    [java.text SimpleDateFormat]))

(defn now
  "Returns a Java Date Object that
  represents the current date and time"
  []
  (Date.))

(defn before?
  "Expects two Date Objects as arguments. The function returns true if the
  first date comes before the second date and returns false otherwise."
  [^Date first ^Date second]
  (.before first second))

(defn after?
  "Expects two Date Objects as arguments. The function returns true if the
  first date comes after the second date and returns false otherwise."
  [^Date first ^Date second]
  (.after first second))

(defn between?
  "Expects the three Date Objects as arguments. The first date is the date
  being evaluated; the second date is the start date; the last date is the
  end date. The function returns true if the first date is between the start
  and end dates."
  [^Date date ^Date start ^Date end]
  (and
    (after? date start)
    (before? date end)))


(defn- mod-time-by-units
  "Modifies the value of a Date object. Expects the first argument to be
  a Date object, the second argument to be a vector representing the amount
  of time to be changed, and the last argument to be either a + or - (indicating
  which direction to modify time)."
  [time [unit n] direction]
  (let [calendar (GregorianCalendar.)
        n (direction n)]
    (.setTime calendar time)
    (.add calendar unit n)
    (.getTime calendar)))

(defn- mod-time
  "Modifies the value of a Date object. Expects the first argument to be
  a Date object, the second argument to be an amount of milliseconds, and
  the last argument to be either a + or - (indicating which direction to
  modify time)."
  [time bit direction]
  (cond
    (number? bit) (Date. (direction (.getTime time) bit))
    (vector? bit) (mod-time-by-units time bit direction)))

(defn before
  "Rewinds the time on a Date object. Expects a Date object as the first
  argument and a number of milliseconds to rewind time by."
  [^Date time & bits]
  (reduce #(mod-time %1 %2 -) time bits))


(defn after
  "Fast-forwards the time on a Date object. Expects a Date object as the first
  argument and a number of milliseconds to fast-forward time by."
  [^Date time & bits]
  (reduce #(mod-time %1 %2 +) time bits))

(defn seconds
  "Converts seconds to milliseconds"
  [n] (* n 1000))


(defn seconds-ago
  "Returns a Java Date Object with a value of n seconds ago where
  n is the value passed to the function."
  [n]
  (before (now) (seconds n)))

(defn seconds-from-now
  "Returns a Java Date Object with a value of n seconds from now where
  n is the value passed to the function."
  [n]
  (after (now) (seconds n)))