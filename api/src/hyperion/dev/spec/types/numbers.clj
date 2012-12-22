(ns hyperion.dev.spec.types.numbers
  (:require [speclj.core :refer :all]
            [hyperion.dev.spec-helper :refer :all]
            [hyperion.api :refer [save save* find-by-kind find-by-key]]))

(defn it-handles-numbers [field caster values pivot-value]

  (list
    (context "saving"
      (for [value values]
        (it (str "saves " (pr-str value))
          (let [record (save {:kind :types field value})]
            (should= value (field (find-by-key (:key record)))))))

      )

    (context "find"

      (def non-nils (keep identity values))
      (def first-and-last [(first values) (last values)])
      (def middles (keep identity (rest (reverse (rest (reverse values))))))

      (before
        (apply save* (map
                       (fn [value]
                         {:kind :types field value})
                       values)))

      (defn field-values [records]
        (map field records))

      (defn field-results
        ([filters]
          (field-values (find-by-kind :types :filters filters)))
        ([op value]
          (field-results [op field value])))

      (it "filters on equality"
        (doseq [value values]
          (should= [value] (field-results := value))))

      (it "filters on inequality"
        (doseq [value values]
          (should=coll (remove #(= value %) values) (field-results :!= value)))
        (should=coll
          (conj first-and-last nil)
          (field-results (map (fn [value] [:!= field value]) middles))))

      (it "filters on <"
        (should=coll (remove #(or (nil? %) (<= % pivot-value)) values) (field-results :> pivot-value)))

      (it "filters on >="
        (should=coll (remove #(or (nil? %) (< % pivot-value)) values) (field-results :>= pivot-value)))

      (it "filters on >="
        (should=coll (remove #(or (nil? %) (>= % pivot-value)) values) (field-results :< pivot-value)))

      (it "filters on >="
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

      (defn sort-result [order]
        (field-values (find-by-kind :types
                              :sorts [field order]
                              :filters [:!= field nil])))

      (it "sorts ascending"
        (should= non-nils (sort-result :asc)))

      (it "sorts descending"
        (should= (reverse non-nils) (sort-result :desc)))

      (defn limit-result [limit offset]
        (field-values (find-by-kind :types
                              :sorts [field :asc]
                              :limit limit
                              :offset offset
                              :filters [:!= field nil])))

      (it "applies limit"
        (should= (take 2 non-nils) (limit-result 2 nil)))

      (it "applies offset"
        (should= (drop 2 non-nils) (limit-result nil 2)))

      (it "applies limit and offset"
        (should= (take 4 (drop 2 non-nils)) (limit-result 4 2)))

      )

    ))

(defn it-handles-ints []
  (let [type-caster (fn [value] (when value (int value)))]
    (it-handles-numbers
      :inti
      type-caster
      (map type-caster [Integer/MIN_VALUE -42 -1 0 nil 1 42 Integer/MAX_VALUE])
      (type-caster 0))))
