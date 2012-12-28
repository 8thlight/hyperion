(ns hyperion.riak.index-optimizer-spec
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [ds]]
            [hyperion.filtering :refer [make-filter] :as filter]
            [hyperion.riak.spec-helper :refer [with-testable-riak-datastore]]
            [hyperion.riak.index-optimizer :refer [build-mr expand-filter index-type build-value-query]])
  (:import  [com.basho.riak.client.query IndexMapReduce BucketMapReduce]))

(def pending-message "secondary indexes on integers is turned off until https://github.com/basho/riak-java-client/issues/112 is fixed")

(defn pend-int [] (pending pending-message))

(describe "filter optimization"
  (context "index-type"
    (it "returns index type int for an integer"
      (pend-int)
      (should= :int (index-type (int 1))))

    (it "returns nil for any number not an integer"
      ; BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, Short
      (should-be-nil (index-type (short 1)))
      (should-be-nil (index-type (long 1)))
      (should-be-nil (index-type (float 1.0)))
      (should-be-nil (index-type (double 1.0)))
      (should-be-nil (index-type (byte 1)))
      (should-be-nil (index-type (bigint 1)))
      (should-be-nil (index-type (bigdec 1))))

    (it "returns bin for strings"
      (should= :bin (index-type "value")))

    (it "returns int for a string representing an int"
      (pend-int)
      (should= :int (index-type "1")))

    (it "returns bin for a collection of two bin values"
      (should= :bin (index-type ["value1" "value2"])))

    (it "returns int for a collection of two int values"
      (pend-int)
      (should= :int (index-type [(int 1) (int 1)])))

    (it "returns nil for nil"
      (should-be-nil (index-type nil)))

    (it "returns int for a collection of int value and one int string"
      (pend-int)
      (should= :int (index-type [(int 1) "1"])))

    (it "returns nil for a collection of int value and one bin value"
      (should-be-nil (index-type [(int 1) "asdf"])))

    (it "returns nil for a collection of int value and one long value"
      (should-be-nil (index-type [(int 1) (long 1)])))

    )

  (context "build-value-query"
    (with bin-filter (make-filter := :bin-field "1"))
    (with int-filter (make-filter := :int-field (int 1)))
    (with int-gt-filter (make-filter :> :int-field (int 10)))
    (with int-lt-filter (make-filter :< :int-field (int 1)))
    (with bin-gt-filter (make-filter :> :bin-field "10"))
    (with bin-lt-filter (make-filter :< :bin-field "1"))

    (it "builds a bin value query"
      (let [query (build-value-query :bin "bucket_name" @bin-filter)]
        (should= "bucket_name" (.getBucket query))
        (should= "bin-field_bin" (.getIndex query))
        (should= "1" (.getValue query))))

    (it "builds an int value query"
      (let [query (build-value-query :int "other_bucket_name" @int-filter)]
        (should= "other_bucket_name" (.getBucket query))
        (should= "int-field_int" (.getIndex query))
        (should= (int 1) (.getValue query))))

    (it "builds an int range query"
      (let [query (build-value-query :int-range "b" [@int-lt-filter @int-gt-filter])]
        (should= "b" (.getBucket query))
        (should= "int-field_int" (.getIndex query))
        (should= (int 1) (.from query))
        (should= (int 10) (.to query))))

    (it "builds a bin range query"
      (let [query (build-value-query :bin-range "b" [@bin-lt-filter @bin-gt-filter])]
        (should= "b" (.getBucket query))
        (should= "bin-field_bin" (.getIndex query))
        (should= "1" (.from query))
        (should= "10" (.to query))))

    )

  (with-testable-riak-datastore)
  (with client (.client (ds)))
  (with equal-filter (make-filter := :bin "asdf"))
  (with bad-equal-filter (make-filter := :bin2 (long 2)))
  (with gt-filter (make-filter :> :bin "asdf"))
  (with lt-filter (make-filter :< :bin "asdf2"))
  (with bad-lt-filter (make-filter :< :bin nil))
  (with bin-lt-filter (make-filter :< :bn "2"))
  (with bin-gt-filter (make-filter :> :bn "2"))

  (it "builds a bucket query when there are no filters"
    (let [[mr filters] (build-mr @client [] "test")]
      (should= [] filters)
      (should= BucketMapReduce (type mr))
      (should= "test" (.getBucket mr))))

  (context "optimizes for an equals query"
    (it "builds an equals query when there is an equals filters"
      (let [[mr filters] (build-mr @client [@equal-filter] "test")]
        (should= [] filters)
        (should= IndexMapReduce (type mr))))

    (it "builds an equals query with remaining filters"
      (let [[mr filters] (build-mr @client [@equal-filter @bad-equal-filter] "test")]
        (should= [@bad-equal-filter] filters)
        (should= IndexMapReduce (type mr))))

    (it "builds an equals query with remaining filters"
      (let [[mr filters] (build-mr @client [@gt-filter @equal-filter @bad-equal-filter] "test")]
        (should== [@gt-filter @bad-equal-filter] filters)
        (should= IndexMapReduce (type mr))))

    (it "does not build an equals query when the filter value has no index type"
      (let [[mr filters] (build-mr @client [@bad-equal-filter] "test")]
        (should= [@bad-equal-filter] filters)
        (should= BucketMapReduce (type mr))))

    (it "does not build an equals query when the filter value is nil"
      (let [nil-filter (make-filter := :int nil)
            [mr filters] (build-mr @client [nil-filter] "test")]
        (should= [nil-filter] filters)
        (should= BucketMapReduce (type mr))))

    )

  (context "optimizes for a range query"
    (with not-gt (make-filter :!= (filter/field @gt-filter) (filter/value @gt-filter)))
    (with not-lt (make-filter :!= (filter/field @lt-filter) (filter/value @lt-filter)))

    (it "builds a range query when there is a lt and gt filter"
      (let [[mr filters] (build-mr @client [@gt-filter @lt-filter] "test")]
        (should== [@not-gt @not-lt] filters)
        (should= IndexMapReduce (type mr))))

    (it "doesn't build a range query when the fields don't match"
      (let [[mr filters] (build-mr @client [@gt-filter @bin-lt-filter] "test")]
        (should= [@gt-filter @bin-lt-filter] filters)
        (should= BucketMapReduce (type mr))))

    (it "doesn't build a range query when the value types don't match"
      (let [[mr filters] (build-mr @client [@bad-lt-filter @gt-filter] "test")]
        (should== [@gt-filter @bad-lt-filter] filters)
        (should= BucketMapReduce (type mr))))

    (it "builds a range query when one field match has the same type and another does not"
      (let [[mr filters] (build-mr @client [@gt-filter @bad-lt-filter @lt-filter] "test")
            ]
        (should== [@bad-lt-filter @not-gt @not-lt] filters)
        (should= IndexMapReduce (type mr))))
    )

  (context "optimizes for a gte query"
    (with gte-filter (make-filter :>= :int "asdf"))
    (with bin-gte-filter (make-filter :>= :bin "1"))
    (with bad-gte-filter (make-filter :>= :int (long 1)))

    (it "builds a range query when there is a gte filter"
      (let [[mr filters] (build-mr @client [@gte-filter] "test")]
        (should== [] filters)
        (should= IndexMapReduce (type mr))))

    (it "doesn't build when value has no index type"
      (let [[mr filters] (build-mr @client [@bad-gte-filter] "test")]
        (should== [@bad-gte-filter] filters)
        (should= BucketMapReduce (type mr))))

    (it "ignores invalid gte filters and uses the good one"
      (let [[mr filters] (build-mr @client [@bad-gte-filter @gte-filter] "test")]
        (should== [@bad-gte-filter] filters)
        (should= IndexMapReduce (type mr))))

    (it "expands a int gte filter to a range filter"
      (pend-int)
      (should= [(make-filter :< :int Integer/MAX_VALUE)
                (make-filter :> :int (int 1))] (expand-filter :int @gte-filter)))

    (it "expands a bin gte filter to a range filter"
      (should= [(make-filter :< :bin "zzzzz")
                (make-filter :> :bin "1")] (expand-filter :bin @bin-gte-filter)))

    )

  (context "optimizes for a lte query"
    (with lte-filter (make-filter :<= :int "asdf"))
    (with bin-lte-filter (make-filter :<= :bin "1"))
    (with bad-lte-filter (make-filter :<= :int (long 1)))

    (it "builds a range query when there is a lte filter"
      (let [[mr filters] (build-mr @client [@lte-filter] "test")]
        (should== [] filters)
        (should= IndexMapReduce (type mr))))

    (it "doesn't build when value has no index type"
      (let [[mr filters] (build-mr @client [@bad-lte-filter] "test")]
        (should== [@bad-lte-filter] filters)
        (should= BucketMapReduce (type mr))))

    (it "ignores invalid lte filters and uses the good one"
      (let [[mr filters] (build-mr @client [@bad-lte-filter @lte-filter] "test")]
        (should== [@bad-lte-filter] filters)
        (should= IndexMapReduce (type mr))))

    (it "expands a int lte filter to a range filter"
      (pend-int)
      (should= [(make-filter :< :int (int 1))
                (make-filter :> :int Integer/MIN_VALUE)] (expand-filter :int @lte-filter)))

    (it "expands a bin lte filter to a range filter"
      (should= [(make-filter :< :bin "1")
                (make-filter :> :bin "")] (expand-filter :bin @bin-lte-filter)))

    )

  )
