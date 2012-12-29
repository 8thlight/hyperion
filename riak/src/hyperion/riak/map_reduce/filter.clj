(ns hyperion.riak.map-reduce.filter
  (:require [clojure.string :as str]
            [cheshire.core :refer [generate-string]]
            [chee.coerce :refer [->string]]
            [fleet :refer [fleet]]
            [hyperion.filtering :as filter :refer [make-filter]]
            [hyperion.riak.map-reduce.big-number :refer [big-number-js]]
            [hyperion.riak.map-reduce.helper :refer [deftemplate-fn parse-number]])
  (:import  [clojure.lang IPersistentCollection]
            [java.text DecimalFormat]))

(defprotocol ComparableJs
  (to-js [this]))

(extend-protocol ComparableJs
  nil
  (to-js [this] (generate-string nil))

  java.lang.Object
  (to-js [this] (generate-string this))

  java.lang.Number
  (to-js [this] (generate-string (str this)))

  clojure.lang.IPersistentCollection
  (to-js [this]
    (str "[" (str/join ", " (map to-js this)) "]"))

  )

(defn- filter-json [filter]
  (to-js (filter/value filter)))

(defn- compare-number-js [filter append]
  (format
    (str "if (isNull(fieldValue)) {return [];} else {try{c = new BigNumber(" (filter-json filter) ").cmp(new BigNumber(fieldValue))}catch(err){return [];};if (!(%s)) {return [];}}")
    append))

(defn- compare-object-js [filter]
  (str "
    if (isNull(fieldValue) || !(fieldValue " (->string (filter/operator filter)) " " (filter-json filter) ")) {
      return [];
    }
    "))

(defmulti -comparator-js (fn [filter] [(type (filter/value filter)) (filter/operator filter)]))

(defmethod -comparator-js [Number :>] [filter]
  (compare-number-js filter "c === -1"))

(defmethod -comparator-js [Object :>] [filter]
  (compare-object-js filter))

(defmethod -comparator-js [Number :>=] [filter]
  (compare-number-js filter "c === -1 || c === 0"))

(defmethod -comparator-js [Object :>=] [filter]
  (compare-object-js filter))

(defmethod -comparator-js [Number :<] [filter]
  (compare-number-js filter "c === 1"))

(defmethod -comparator-js [Object :<] [filter]
  (compare-object-js filter))

(defmethod -comparator-js [Number :<=] [filter]
  (compare-number-js filter "c === 1 || c === 0"))

(defmethod -comparator-js [Object :<=] [filter]
  (compare-object-js filter))

(defmethod -comparator-js [Number :=] [filter]
  (compare-number-js filter "c === 0"))

(defmethod -comparator-js [Object :=] [filter]
  (str "if (fieldValue !== " (filter-json filter) ") {return [];}"))

(defmethod -comparator-js [IPersistentCollection :contains?] [filter]
  (str "if (!any(fieldValue, " (filter-json filter) ")) {return [];}"))

(defmethod -comparator-js [nil :=] [filter]
  "if (!isNull(fieldValue)) {return [];}")

(defmethod -comparator-js [Object :!=] [filter]
  (str "if (fieldValue === " (filter-json filter) ") {return [];}"))

(defmethod -comparator-js [nil :!=] [filter]
  "if (isNull(fieldValue)) {return [];}")

(defprotocol CoerceFilterValue
  (coerce-filter-value [this]))

(extend-protocol CoerceFilterValue
  nil
  (coerce-filter-value [this] nil)

  java.lang.Object
  (coerce-filter-value [this] this)

  java.lang.String
  (coerce-filter-value [this]
    (if-let [num (parse-number this)]
      num
      this))

  clojure.lang.IPersistentCollection
  (coerce-filter-value [this]
    (map coerce-filter-value this))

  )

(defn comparator-js [filter]
  (-comparator-js (make-filter
                    (filter/operator filter)
                    (filter/field filter)
                    (coerce-filter-value (filter/value filter)))))

(defn needs-big-number-js? [filters]
  (some #(number? (coerce-filter-value (filter/value %))) filters))

(defn needs-contain-js? [filters]
  (some #(= :contains? (filter/operator %)) filters))

(def contain-js
  "
  function any(value, coll) {
    for (var i = 0; i < coll['length']; i++) {
      if (value === coll[i]) {
        return true;
      }
    }
    return false;
  };
  ")

(def filter-template
  "
  function f(riakRecord) {
    var x;
    <(if (needs-big-number-js? filters) big-number-js \">x=1;<\")>
    <(if (needs-contain-js? filters) contain-js \">x=1;<\")>

    var fieldValue, c;
    var data = Riak.mapValuesJson(riakRecord)[0];

    function isNull(value) {
      return (typeof value === 'undefined' || value === null);
    };

    <(for [filter filters] \">
      fieldValue = data[<(raw (generate-string (filter/field filter)))>];
      <(comparator-js filter)>
    <\")>

    data['id'] = riakRecord['key'];
    return [data];
  }
  ")

(deftemplate-fn filter-js (fleet [filters] filter-template {:escaping :bypass}))

