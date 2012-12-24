(ns hyperion.riak.map-reduce.count
  (:require [fleet :refer [fleet]]
            [hyperion.riak.map-reduce.helper :refer [deftemplate-fn]]))

(def count-template
"
function f(values) {
  return [values.length];
}
  ")

(deftemplate-fn count-js (fleet [] count-template {:escaping :bypass}))


