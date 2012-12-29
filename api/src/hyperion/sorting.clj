(ns hyperion.sorting)

(defn field [[field _]] field)

(defn order [[_ order]] order)

(defn make-sort [field order]
  [field order])

(defprotocol FakeComparable
  (compare-to [this other]))

(extend-protocol FakeComparable
  java.lang.Object
  (compare-to [this other] (.compareTo this other))

  ; why can't BigInt just implement compareTo like every other java class...
  clojure.lang.BigInt
  (compare-to [this other]
    (.compareTo (.toBigInteger this) (.toBigInteger other)))

  )


(defn- ->compare-fn [sort]
  (let [field (field sort)
        multiplier (if (= :desc (order sort)) -1 1)]
    (fn [a b]
      (* multiplier
        (let [av (get a field)
              bv (get b field)]
          (cond
            (and (nil? av) (nil? bv)) 0
            (nil? av) 1
            (nil? bv) -1
            :else (compare-to av bv)))))))

(deftype Comp [compare-fns]
  java.util.Comparator
  (compare [this a b]
    (or
      (some
        #(if (zero? %) false %)
        (map (fn [compare-fn] (compare-fn a b)) compare-fns))
      0)))

(defn- build-comparator [sorts]
  (let [compare-fns (map ->compare-fn sorts)]
    (Comp. compare-fns)))

(defn sort-results [sorts results]
  (if (empty? sorts)
    results
    (sort (build-comparator sorts) results)))
