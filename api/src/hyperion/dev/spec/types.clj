(ns hyperion.dev.spec.types
  (:require [speclj.core :refer :all]
            [hyperion.api :refer [save save* find-by-kind find-by-key]]))

(defn- filter-field-results-fn [field-values]
  (fn [filters]
    (field-values
      (find-by-kind :types :filters filters))))

(defn- field-results-fn [field filter-field-results]
  (fn
    ([filters]
     (filter-field-results filters))
    ([op value]
     (filter-field-results [op field value]))))

(defn- field-values-fn [field]
  (fn [records] (map field records)))

(defn- sort-result-fn [field field-values]
  (fn [order]
    (field-values (find-by-kind :types
                                :sorts [field order]
                                :filters [:!= field nil]))))

(defn- limit-result-fn [field field-values]
  (fn [limit offset]
    (field-values (find-by-kind :types
                                :sorts [field :asc]
                                :limit limit
                                :offset offset
                                :filters [:!= field nil]))))

(defn- lte [value1 value2]
  (if (number? value1)
    (and (not (nil? value2)) (<= value1 value2))
    (and (not (nil? value2)) (<= (.compareTo value1 value2) 0))))

(defn- lt [value1 value2]
  (if (number? value1)
    (and (not (nil? value2)) (< value1 value2))
    (and (not (nil? value2)) (< (.compareTo value1 value2) 0))))

(defn- gte [value1 value2]
  (if (number? value1)
    (and (not (nil? value2)) (>= value1 value2))
    (and (not (nil? value2)) (>= (.compareTo value1 value2) 0))))

(defn- gt [value1 value2]
  (if (number? value1)
    (and (not (nil? value2)) (> value1 value2))
    (and (not (nil? value2)) (> (.compareTo value1 value2) 0))))

(defn- build-type-checker [klass]
  (fn [value]
    (if (nil? value)
      true
      (should= klass (type value)))))

(defn- it-finds [field type-checker values pivot-value]
  (let [non-nils (keep identity values)
        first-and-last [(first values) (last values)]
        middles (keep identity (rest (reverse (rest (reverse values)))))
        field-values (field-values-fn field)
        filter-field-results (filter-field-results-fn field-values)
        field-results (field-results-fn field filter-field-results)
        sort-result (sort-result-fn field field-values)
        limit-result (limit-result-fn field field-values)]
    (list
      (context "saving"
        (tags :save)
        (for [value values]
          (it (str "saves " (pr-str value))
            (let [record (save {:kind :types field value})
                  saved-value (field (find-by-key (:key record)))]
              (should= value saved-value)
              (type-checker saved-value))))

        )

      (context "find"
        (tags :find)

        (before
          (apply save* (map
                         (fn [value]
                           {:kind :types field value})
                         values)))

        (it "filters on equality"
          (doseq [value values]
            (let [result (field-results := value)]
              (should= [value] result)
              (type-checker (first result)))))

        (it "filters on inequality"
          (doseq [value values]
            (should== (remove #(= value %) values) (field-results :!= value)))
          (should==
            (conj first-and-last nil)
            (field-results (map (fn [value] [:!= field value]) middles))))

        (it "filters on >"
          (should== (remove #(or (nil? %) (lte % pivot-value)) values) (field-results :> pivot-value)))

        (it "filters on >="
          (should== (remove #(or (nil? %) (lt % pivot-value)) values) (field-results :>= pivot-value)))

        (it "filters on <"
          (should== (remove #(or (nil? %) (gte % pivot-value)) values) (field-results :< pivot-value)))

        (it "filters on <="
          (should== (remove #(or (nil? %) (gt % pivot-value)) values) (field-results :<= pivot-value)))

        (it "filters on inclusion"
          (doseq [value values]
            (should= [value] (field-results :in [value])))
          (should== first-and-last (field-results :in first-and-last))
          (should== middles (field-results :in middles)))

        (it "filters on a range"
          (should==
            middles
            (field-results [[:> field (first values)] [:< field (last values)]])))

        (it "sorts ascending"
          (should= non-nils (sort-result :asc)))

        (it "sorts descending"
          (should= (reverse non-nils) (sort-result :desc)))

        (it "applies limit"
          (should= (take 2 non-nils) (limit-result 2 nil)))

        (it "applies offset"
          (should= (drop 2 non-nils) (limit-result nil 2)))

        (it "applies limit and offset"
          (should= (take 4 (drop 2 non-nils)) (limit-result 4 2)))

        )

      )))

(def big-long-min (BigInteger. (str Long/MIN_VALUE)))
(def big-long-max (BigInteger. (str Long/MAX_VALUE)))

(def float-min (* -1 Float/MAX_VALUE))

(defn it-handles-types []
  (list
    (context "booleans"
      (tags :bool)

      (context "saving"
        (for [value [true false nil]]
          (it (str "saves " value)
            (let [record (save {:kind :types :bool value})]
              (should= value (:bool (find-by-key (:key record))))))))

      (context "find"
        (before
          (save*
            {:kind :types :bool true}
            {:kind :types :bool false}
            {:kind :types :bool nil}))

        (defn -result-count [value op]
          (count (find-by-kind :types :filters [op :bool value])))

        (defn result-count [value]
          (-result-count value :=))

        (defn not-result-count [value]
          (-result-count value :!=))

        (for [value [true false nil]]
          (list
            (it (str "finds " (pr-str value))
              (should= 1 (result-count value)))

            (it (str "finds not " (pr-str value))
              (should= 2 (not-result-count value)))))

        (it "finds with contains?"
          (should== [true] (map :bool (find-by-kind :types :filters [:in :bool [true]])))
          (should== [true false] (map :bool (find-by-kind :types :filters [:in :bool [true false]])))
          (should== [true nil] (map :bool (find-by-kind :types :filters [:in :bool [true nil]])))
          (should== [false] (map :bool (find-by-kind :types :filters [:in :bool [false]])))
          (should== [false nil] (map :bool (find-by-kind :types :filters [:in :bool [false nil]])))
          (should== [nil] (map :bool (find-by-kind :types :filters [:in :bool [nil]]))))))

    (context "bytes"
       (tags :byte)
       (let [type-caster (fn [value] (when value (byte value)))]
         (it-finds
           :bite
           (build-type-checker Byte)
           (map type-caster [Byte/MIN_VALUE -42 -1 0 nil 1 42 Byte/MAX_VALUE])
           (type-caster 0))))

    (context "shorts"
       (tags :short)
       (let [type-caster (fn [value] (when value (short value)))]
         (it-finds
           :shrt
           (build-type-checker Short)
           (map type-caster [Short/MIN_VALUE (dec Byte/MIN_VALUE) -42 0 nil 42 (inc Byte/MAX_VALUE) Short/MAX_VALUE])
           (type-caster 0))))

    (context "integers"
      (tags :int)
      (let [type-caster (fn [value] (when value (int value)))]
        (it-finds
          :inti
          (build-type-checker Integer)
          (map type-caster [Integer/MIN_VALUE (dec Short/MIN_VALUE) -42 0 nil 42 (inc Short/MAX_VALUE) Integer/MAX_VALUE])
          (type-caster 0))))

    (context "longs"
      (tags :long)
      (let [type-caster (fn [value] (when value (long value)))]
        (it-finds
          :lng
          (build-type-checker Long)
          (map type-caster [Long/MIN_VALUE (dec Integer/MIN_VALUE) -42 0 nil 42 (inc Integer/MAX_VALUE) Long/MAX_VALUE])
          (type-caster 0))))

    (context "floats"
      (tags :float)
      (let [type-caster (fn [value] (when value (float value)))]
        (it-finds
          :flt
          (build-type-checker Float)
          (map type-caster [float-min -42.010 -1.1098 0.0 nil 1.98 42.89 Float/MAX_VALUE])
          (type-caster 0))))

    (context "doubles"
      (tags :double)
      (let [type-caster (fn [value] (when value (double value)))]
        (it-finds
          :dbl
          (build-type-checker Double)
          (map type-caster [(* -1 Double/MAX_VALUE) float-min -42.010 0.0 nil 42.89 (inc Float/MAX_VALUE) Double/MAX_VALUE])
          (type-caster 0))))

    (context "strings"
      (tags :string)
      (let [type-caster (fn [value] (when value (str value)))]
        (it-finds
          :str
          (build-type-checker String)
          (map type-caster ["a" "aa" "b" "f" "g" nil "o" "x" "z"])
          (type-caster "g"))))

    (context "characters"
      (tags :char)
      (let [type-caster (fn [value] (when value (.charAt (str value) 0)))]
        (it-finds
          :chr
          (build-type-checker Character)
          (map type-caster ["a" "b" "f" "g" nil "o" "x" "z"])
          (type-caster "g"))))

    (context "kewords"
      (tags :keyword)
      (let [type-caster (fn [value] (when value (keyword value)))]
        (it-finds
          :kwd
          (build-type-checker clojure.lang.Keyword)
          (map type-caster [:a :aa :b :f :g nil :o :x :z])
          (type-caster :g))))

    ))
