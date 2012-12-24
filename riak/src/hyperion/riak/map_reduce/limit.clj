(ns hyperion.riak.map-reduce.limit
  (:require [cheshire.core :refer [generate-string]]
            [fleet :refer [fleet]]
            [hyperion.riak.map-reduce.helper :refer [deftemplate-fn]]))

(def limit-template
"
function f(values) {
  return values.slice(0, <(generate-string limit)>);
}
  ")

(deftemplate-fn limit-js (fleet [limit] limit-template {:escaping :bypass}))
