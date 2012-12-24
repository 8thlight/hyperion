(ns hyperion.riak.map-reduce.filter
  (:require [clojure.string :as str]
            [cheshire.core :refer [generate-string]]
            [chee.coerce :refer [->string]]
            [fleet :refer [fleet]]
            [hyperion.filtering :as filter]
            [hyperion.riak.map-reduce.big-number :refer [big-number-js]]
            [hyperion.riak.map-reduce.json-parse :refer [json-parse-js]]
            [hyperion.riak.map-reduce.helper :refer [deftemplate-fn]])
  (:import  [clojure.lang IPersistentCollection]))

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
    (str "if (isNull(fieldValue)) {return [];} else {c = new BigNumber(" (filter-json filter) ").cmp(new BigNumber(fieldValue));if (!(%s)) {return [];}}")
    append))

(defn- compare-object-js [filter]
  (str "
    if (isNull(fieldValue) || !(fieldValue " (->string (filter/operator filter)) " " (filter-json filter) ")) {
      return [];
    }
    "))

(defmulti comparator-js (fn [filter] [(type (filter/value filter)) (filter/operator filter)]))

(defmethod comparator-js [Number :>] [filter]
  (compare-number-js filter "c === -1"))

(defmethod comparator-js [Object :>] [filter]
  (compare-object-js filter))

(defmethod comparator-js [Number :>=] [filter]
  (compare-number-js filter "c === -1 || c === 0"))

(defmethod comparator-js [Object :>=] [filter]
  (compare-object-js filter))

(defmethod comparator-js [Number :<] [filter]
  (compare-number-js filter "c === 1"))

(defmethod comparator-js [Object :<] [filter]
  (compare-object-js filter))

(defmethod comparator-js [Number :<=] [filter]
  (compare-number-js filter "c === 1 || c === 0"))

(defmethod comparator-js [Object :<=] [filter]
  (compare-object-js filter))

(defmethod comparator-js [Object :=] [filter]
  (str "if (fieldValue !== " (filter-json filter) ") {return [];}"))

(defmethod comparator-js [IPersistentCollection :contains?] [filter]
  (str "if (!any(fieldValue, " (filter-json filter) ")) {return [];}"))

(defmethod comparator-js [nil :=] [filter]
  "if (!isNull(fieldValue)) {return [];}")

(defmethod comparator-js [Object :!=] [filter]
  (str "if (fieldValue === " (filter-json filter) ") {return [];}"))

(defmethod comparator-js [nil :!=] [filter]
  "if (isNull(fieldValue)) {return [];}")

(def filter-template
  (str
"
function f(riakRecord) {
"
big-number-js
json-parse-js
"
  var fieldValue, c;
  var data = json_parse(riakRecord['values'][0]['data']);

  function isNull(value) {
    return (typeof value === 'undefined' || value === null);
  };

  function any(value, coll) {
    for (var i = 0; i < coll['length']; i++) {
      if (value === coll[i]) {
        return true;
      }
    }
    return false;
  };

  <(for [filter filters] \">
    fieldValue = data[<(raw (generate-string (filter/field filter)))>];
    <(comparator-js filter)>
  <\")>

  data['id'] = riakRecord['key'];
  return [data];
}
  "))

(def filter-js (fleet [filters] filter-template {:escaping :bypass}))

