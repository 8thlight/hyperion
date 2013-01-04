(ns hyperion.dev.spec.types.strings
  (:require [hyperion.dev.spec.types.helper :refer [it-finds build-type-checker]]))

(defn it-handles-strings []
  (let [type-caster (fn [value] (when value (str value)))]
    (it-finds
      :str
      (build-type-checker String)
      (map type-caster ["a" "aa" "b" "f" "g" nil "o" "x" "z"])
      (type-caster "g"))))

