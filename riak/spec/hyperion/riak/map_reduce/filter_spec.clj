(ns hyperion.riak.map-reduce.filter-spec
  (:require [speclj.core :refer :all]
            [hyperion.filtering :refer [make-filter]]
            [hyperion.riak.map-reduce.filter :refer [needs-big-number-js? needs-contain-js?]]))

(describe "map-reduce filter"

  (defn num-filter [num]
    (make-filter := :num num))

  (it "needs big number js if there is a filter with a number in it"
    (should (needs-big-number-js? [(num-filter (short 1))]))
    (should (needs-big-number-js? [(num-filter (long 1))]))
    (should (needs-big-number-js? [(num-filter (float 1))]))
    (should (needs-big-number-js? [(num-filter (double 1))]))
    (should (needs-big-number-js? [(num-filter (byte 1))]))
    (should (needs-big-number-js? [(num-filter (bigint 1))]))
    (should (needs-big-number-js? [(num-filter (bigdec 1))])))

  (it "does not need big number js if there are no filters if numbers"
    (should-not (needs-big-number-js? [(num-filter "1")]))
    (should-not (needs-big-number-js? [])))

  (it "needs contain js if there is a filter with a :contains? operator"
    (should-not (needs-contain-js? []))
    (should-not (needs-contain-js? [(num-filter "1")])))

  )
