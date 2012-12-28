(ns hyperion.riak.index-optimizer
  (:require [chee.coerce :refer [->int]]
            [hyperion.filtering :as filter :refer [make-filter]]
            [hyperion.riak.map-reduce.helper :refer [parse-number]])
  (:import  [com.basho.riak.client.query IndexMapReduce BucketMapReduce]
            [com.basho.riak.client.query.indexes BinIndex IntIndex]
            [com.basho.riak.client.raw.query.indexes BinValueQuery BinRangeQuery IntValueQuery IntRangeQuery]
            [clojure.lang IPersistentCollection]))

; secondary indexes on integers is turned off until
; https://github.com/basho/riak-java-client/issues/112 is fixed

(defprotocol IndexType
  (index-type [this]))

(extend-protocol IndexType
  ;Integer
  ;(index-type [this] :int)

  Number
  (index-type [this] nil)

  IPersistentCollection
  (index-type [this]
    (let [types (map index-type this)]
      (if (apply = types)
        (first types)
        nil)))

  String
  (index-type [this]
    (when-not (number? (parse-number this))
      :bin))
    ;(try
    ;  (Integer/parseInt this)
    ;  :int
    ;  (catch NumberFormatException _
    ;    :bin)))

  Object
  (index-type [this] :bin)

  nil
  (index-type [this] nil)

  )

(defmulti build-value-query (fn [type _ _] type))

(defmethod build-value-query :int [_ bucket-name filter]
  (IntValueQuery.
    (IntIndex/named
      (name (filter/field filter)))
    bucket-name
    (->int (filter/value filter))))

(defmethod build-value-query :int-range [_ bucket-name [lt-filter gt-filter]]
  (IntRangeQuery.
    (IntIndex/named
      (name (filter/field lt-filter)))
    bucket-name
    (->int (filter/value lt-filter))
    (->int (filter/value gt-filter))))

(defmethod build-value-query :bin [_ bucket-name filter]
  (BinValueQuery.
    (BinIndex/named
      (name (filter/field filter)))
    bucket-name
    (str (filter/value filter))))

(defmethod build-value-query :bin-range [_ bucket-name [lt-filter gt-filter]]
  (BinRangeQuery.
    (BinIndex/named
      (name (filter/field lt-filter)))
    bucket-name
    (str (filter/value lt-filter))
    (str (filter/value gt-filter))))

(defn- equals [client filters bucket-name]
  (loop [[filter & more] filters seen []]
    (cond
      (nil? filter) nil
      (= := (filter/operator filter))
        (if-let [idx-type (index-type (filter/value filter))]
          [
           (IndexMapReduce. client (build-value-query idx-type bucket-name filter))
           (concat seen more)
            ]
          (recur more (conj seen filter))
          )
      :else
        (recur more (conj seen filter)))))

(defn- all-with-operator [op filters]
  (filter #(= op (filter/operator %)) filters))

(defn- find-field-matches [to-search to-match-against]
  (keep identity
        (for [search to-search match to-match-against]
          (when (= (filter/field match) (filter/field search))
            [search match]))))

(defn range-type [index-type]
  (keyword (str (name index-type) "-range")))

(defn remove-first [coll value]
  (loop [coll coll seen []]
    (if (empty? coll)
      seen
      (let [f (first coll)]
        (if (= f value)
          (concat seen (rest coll))
          (recur (rest coll) (conj seen f)))))))

(defn- range-q [client filters bucket-name]
  (loop [[match & more] (find-field-matches
                          (all-with-operator :< filters)
                          (all-with-operator :> filters))]
    (when match
      (if-let [type (index-type (map filter/value match))]
        [
         (IndexMapReduce. client (build-value-query (range-type type) bucket-name match))
         (let [[lt gt] match]
           (-> filters
             (remove-first (first match))
             (remove-first (second match))
             (conj (make-filter :!= (filter/field lt) (filter/value lt)))
             (conj (make-filter :!= (filter/field gt) (filter/value gt)))))
         ]
        (recur more)))))


(defmulti expand-filter (fn [type filter] [type (filter/operator filter)]))

(defmethod expand-filter [:int :>=] [_ filter]
  [(make-filter :< (filter/field filter) Integer/MAX_VALUE)
   (make-filter :> (filter/field filter) (filter/value filter))])

(defmethod expand-filter [:int :<=] [_ filter]
  [(make-filter :< (filter/field filter) (filter/value filter))
   (make-filter :> (filter/field filter) Integer/MIN_VALUE)])

(defmethod expand-filter [:bin :>=] [_ filter]
  [(make-filter :< (filter/field filter) "zzzzz")
   (make-filter :> (filter/field filter) (filter/value filter))])

(defmethod expand-filter [:bin :<=] [_ filter]
  [(make-filter :< (filter/field filter) (filter/value filter))
   (make-filter :> (filter/field filter) "")])

(defn- build-or-equal-to [op]
  (fn [client filters bucket-name]
    (loop [[filter & more] (all-with-operator op filters) seen []]
      (when filter
        (if-let [type (index-type (filter/value filter))]
          [
           (IndexMapReduce. client (build-value-query (range-type type) bucket-name (expand-filter type filter)))
           (concat seen more)
           ]
          (recur more (conj seen filter)))))))

(defn- bucket [client filters bucket-name]
  [
   (BucketMapReduce. client bucket-name)
   filters
   ])

(def ^:private optimizers [equals range-q (build-or-equal-to :>=) (build-or-equal-to :<=) bucket])

(defn build-mr [client filters bucket-name]
  (first (remove #(nil? %) (map #(% client filters bucket-name) optimizers))))
