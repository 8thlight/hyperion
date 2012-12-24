(ns hyperion.riak.map-reduce.offset
  (:require [cheshire.core :refer [generate-string]]
            [fleet :refer [fleet]]
            [hyperion.riak.map-reduce.helper :refer [deftemplate-fn]]))

(def offset-template
"
function f(values) {
  return values.slice(<(generate-string offset)>, values.length);
}
  ")

(deftemplate-fn offset-js (fleet [offset] offset-template {:escaping :bypass}))

