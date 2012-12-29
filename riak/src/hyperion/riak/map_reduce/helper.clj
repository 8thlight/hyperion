(ns hyperion.riak.map-reduce.helper
  (:import  [com.google.javascript.jscomp.Compiler]
            [com.google.javascript.jscomp CompilerOptions CompilationLevel JSSourceFile CheckLevel]))

(defn parse-number [s]
  (try
    (Long. s)
    (catch NumberFormatException _
      (try
        (BigInteger. s)
        (catch NumberFormatException _
          (try
            (Double. s)
            (catch NumberFormatException _
              s)))))))

(defn remove-from-end [s end]
  (if (.endsWith s end)
      (.substring s 0 (- (count s) (count end)))
    s))

(defn optimize-js [js]
  (let [compiler (com.google.javascript.jscomp.Compiler.)
        options  (CompilerOptions.)
        extern (JSSourceFile/fromCode "externs.js" "function alert(x) {}")
        input (JSSourceFile/fromCode "input.js" (str js))]
    (.setOptionsForCompilationLevel CompilationLevel/SIMPLE_OPTIMIZATIONS options)
    (.compile compiler extern input options)
    (-> (.toSource compiler)
      (remove-from-end ";"))))

(defn optimized-fn [f]
  (fn [& args]
    (optimize-js (apply f args))))

(defmacro deftemplate-fn [fn-name form]
  `(def ~fn-name (memoize (optimized-fn ~form))))
