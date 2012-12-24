(ns hyperion.dev.spec.types.boolean
  (:require [speclj.core :refer :all ]
            [hyperion.api :refer [save save* find-by-kind find-by-key]]))

(defn it-handles-booleans []
  (list

    (context "saving"

      (for [value [true false nil]]

        (it (str "saves " value)
          (let [record (save {:kind :types :bool value})]
            (should= value (:bool (find-by-key (:key record)))))))

      )

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
            (should= 2 (not-result-count value)))

        ))

      (it "finds with contains?"
        (should== [true] (map :bool (find-by-kind :types :filters [:in :bool [true]])))
        (should== [true false] (map :bool (find-by-kind :types :filters [:in :bool [true false]])))
        (should== [true nil] (map :bool (find-by-kind :types :filters [:in :bool [true nil]])))
        (should== [false] (map :bool (find-by-kind :types :filters [:in :bool [false]])))
        (should== [false nil] (map :bool (find-by-kind :types :filters [:in :bool [false nil]])))
        (should== [nil] (map :bool (find-by-kind :types :filters [:in :bool [nil]]))))

      )

    ))
