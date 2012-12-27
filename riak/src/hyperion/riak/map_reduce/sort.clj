(ns hyperion.riak.map-reduce.sort
  (:require [cheshire.core :refer [generate-string]]
            [fleet :refer [fleet]]
            [hyperion.sorting :as sort]
            [hyperion.riak.map-reduce.big-number :refer [big-number-js]]
            [hyperion.riak.map-reduce.helper :refer [deftemplate-fn]]))

(def sort-template
  "
  function f(values) {
    <(raw big-number-js)>

    function compareAscending(field1, field2) {
      try {
        return new BigNumber(field1).cmp(new BigNumber(field2));
      } catch(err) {
        if (field1 < field2) {
          return -1;
        } else {
          return 1;
        }
      }
    }

    return values.sort(function(record1, record2) {
      var field1, field2;
      <(for [sort sorts]
        \">
        <(let [field (sort/field sort)
               field-json (generate-string field)]
          \">
          field1 = record1[<(raw field-json)>];
          field2 = record2[<(raw field-json)>];
          if (field1 !== field2) {
            <(case (sort/order sort)
              :asc
                \">
                  return compareAscending(field1, field2);
                <\"
              :desc
                \">
                  return compareAscending(field1, field2) * -1;
                <\"
              \">
                return 1;
              <\"
              )>
          }
          <\")>
        <\"
        )>
      return 0;
    });
  }
  ")

(deftemplate-fn sort-js (fleet [sorts] sort-template {:escaping :bypass}))

