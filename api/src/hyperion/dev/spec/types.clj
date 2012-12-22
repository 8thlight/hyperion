(ns hyperion.dev.spec.types
  (:require [speclj.core :refer :all]
            [hyperion.dev.spec.types.boolean :refer [it-handles-booleans]]))

(defn it-handles-types []
  (context "booleans"
    (it-handles-booleans)))
