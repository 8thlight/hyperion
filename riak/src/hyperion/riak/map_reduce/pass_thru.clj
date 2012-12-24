(ns hyperion.riak.map-reduce.pass-thru
  (:require [fleet :refer [fleet]]
            [hyperion.riak.map-reduce.helper :refer [deftemplate-fn]]))

(def pass-thru-template
"
function f(values) {
  return values;
}
  ")

(deftemplate-fn pass-thru-js (fleet [] pass-thru-template {:escaping :bypass}))


