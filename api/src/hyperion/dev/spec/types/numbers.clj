(ns hyperion.dev.spec.types.numbers
  (:require [hyperion.dev.spec.types.helper :refer [it-finds build-type-checker]]))

(defn it-handles-bytes []
  (let [type-caster (fn [value] (when value (byte value)))]
    (it-finds
      :bite
      (build-type-checker Byte)
      (map type-caster [Byte/MIN_VALUE -42 -1 0 nil 1 42 Byte/MAX_VALUE])
      (type-caster 0))))

(defn it-handles-ints []
  (let [type-caster (fn [value] (when value (int value)))]
    (it-finds
      :inti
      (build-type-checker Integer)
      (map type-caster [Integer/MIN_VALUE (dec Short/MIN_VALUE) -42 0 nil 42 (inc Short/MAX_VALUE) Integer/MAX_VALUE])
      (type-caster 0))))

(defn it-handles-longs []
  (let [type-caster (fn [value] (when value (long value)))]
    (it-finds
      :lng
      (build-type-checker Long)
      (map type-caster [Long/MIN_VALUE (dec Integer/MIN_VALUE) -42 0 nil 42 (inc Integer/MAX_VALUE) Long/MAX_VALUE])
      (type-caster 0))))

(def big-long-min (BigInteger. (str Long/MIN_VALUE)))
(def big-long-max (BigInteger. (str Long/MAX_VALUE)))

(def float-min (* -1 Float/MAX_VALUE))

(defn it-handles-floats []
  (let [type-caster (fn [value] (when value (float value)))]
    (it-finds
      :flt
      (build-type-checker Float)
      (map type-caster [float-min -42.010 -1.1098 0.0 nil 1.98 42.89 Float/MAX_VALUE])
      (type-caster 0))))

(defn it-handles-doubles []
  (let [type-caster (fn [value] (when value (double value)))]
    (it-finds
      :dbl
      (build-type-checker Double)
      (map type-caster [(* -1 Double/MAX_VALUE) float-min -42.010 0.0 nil 42.89 (inc Float/MAX_VALUE) Double/MAX_VALUE])
      (type-caster 0))))
