(ns hyperion.dev.spec.types.numbers
  (:require [speclj.core :refer :all]
            [hyperion.dev.spec-helper :refer :all]
            [hyperion.api :refer [save save* find-by-kind find-by-key]]))

(defn filter-field-results-fn [field-values]
  (fn [filters]
    (field-values
      (find-by-kind :types :filters filters))))

(defn field-results-fn [field filter-field-results]
  (fn
    ([filters]
     (filter-field-results filters))
    ([op value]
     (filter-field-results [op field value]))))

(defn field-values-fn [field]
  (fn [records] (map field records)))

(defn sort-result-fn [field field-values]
  (fn [order]
    (field-values (find-by-kind :types
                                :sorts [field order]
                                :filters [:!= field nil]))))

(defn limit-result-fn [field field-values]
  (fn [limit offset]
    (field-values (find-by-kind :types
                                :sorts [field :asc]
                                :limit limit
                                :offset offset
                                :filters [:!= field nil]))))

(defn it-handles-numbers [field caster values pivot-value]
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
        (for [value values]
          (it (str "saves " (pr-str value))
            (let [record (save {:kind :types field value})]
              (should= value (field (find-by-key (:key record)))))))

        )

      (context "find"
        (before
          (apply save* (map
                         (fn [value]
                           {:kind :types field value})
                         values)))

        (it "filters on equality"
          (doseq [value values]
            (should= [value] (field-results := value))))

        (it "filters on inequality"
          (doseq [value values]
            (should=coll (remove #(= value %) values) (field-results :!= value)))
          (should=coll
            (conj first-and-last nil)
            (field-results (map (fn [value] [:!= field value]) middles))))

        (it "filters on >"
          (should=coll (remove #(or (nil? %) (<= % pivot-value)) values) (field-results :> pivot-value)))

        (it "filters on >="
          (should=coll (remove #(or (nil? %) (< % pivot-value)) values) (field-results :>= pivot-value)))

        (it "filters on <"
          (should=coll (remove #(or (nil? %) (>= % pivot-value)) values) (field-results :< pivot-value)))

        (it "filters on <="
          (should=coll (remove #(or (nil? %) (> % pivot-value)) values) (field-results :<= pivot-value)))

        (it "filters on inclusion"
          (doseq [value values]
            (should= [value] (field-results :in [value])))
          (should=coll first-and-last (field-results :in first-and-last))
          (should=coll middles (field-results :in middles)))

        (it "filters on a range"
          (should=coll
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

(defn it-handles-ints []
  (let [type-caster (fn [value] (when value (int value)))]
    (it-handles-numbers
      :inti
      type-caster
      (map type-caster [Integer/MIN_VALUE -42 -1 0 nil 1 42 Integer/MAX_VALUE])
      (type-caster 0))))

(defn it-handles-floats []
  (let [type-caster (fn [value] (when value (Float. (str value))))]
    (it-handles-numbers
      :flt
      type-caster
      (map type-caster [(* -1 Float/MAX_VALUE) -42.010 -1.1098 0.0 nil 1.98 42.89 Float/MAX_VALUE])
      (type-caster 0))))
