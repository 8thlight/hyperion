(ns hyperion.riak.map-reduce.ids
  (:require [fleet :refer [fleet]]
            [hyperion.riak.map-reduce.helper :refer [deftemplate-fn]]))

(def ids-template
"
function f(values) {
  var ids = [];
  for (var i=0; i<values.length; i++) {
    ids.push(values[i].id);
  }
  return ids;
}
  ")

(deftemplate-fn ids-js (fleet [] ids-template {:escaping :bypass}))

