(ns hyperion.dev.spec-helper
  (:require [speclj.core :refer :all]
            [speclj.util :refer [endl]]
            [hyperion.api :refer [*ds*]]
            [hyperion.memory :refer [new-memory-datastore]])
  (:import  [speclj SpecFailure]))

(defn with-memory-datastore []
  (around [it]
    (binding [*ds* (new-memory-datastore)]
      (it))))

;;;; should=coll matcher (until it gets added to speclj) ;;;;

(defn coll-includes? [coll item]
  (some #(= % item) coll))

(defn remove-first [coll value]
  (loop [coll coll seen []]
    (if (empty? coll)
      seen
      (let [f (first coll)]
        (if (= f value)
          (concat seen (rest coll))
          (recur (rest coll) (conj seen f)))))))

(defn coll-difference [coll1 coll2]
  (loop [match-with coll1 match-against coll2 diff []]
    (if (empty? match-with)
      diff
      (let [f (first match-with)
            r (rest match-with)]
        (if (coll-includes? match-against f)
          (recur r (remove-first match-against f) diff)
          (recur r match-against (conj diff f)))))))

(defn difference-message [expected actual extra missing]
  (-> (str "Expected collection contained:  " (prn-str expected) endl "Actual collection contained:    " (prn-str actual))
    (#(if (empty? missing)
       %
       (str % endl "The missing elements were:      " (prn-str missing))))
    (#(if (empty? extra)
       %
       (str % endl "The extra elements were:        " (prn-str extra))))
    ))

(defmacro should=coll
  "Asserts that the collection is contained within another collection"
  [expected actual]
  `(let [extra# (coll-difference ~actual ~expected)
         missing# (coll-difference ~expected ~actual)]
     (if-not (and (empty? extra#) (empty? missing#))
       (let [error-message# (difference-message ~expected ~actual extra# missing#)]
         (throw (SpecFailure. error-message#))))
     )
  )

;;;;
