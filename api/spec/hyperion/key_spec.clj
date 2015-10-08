(ns hyperion.key-spec
  (:require [speclj.core :refer :all]
            [hyperion.key :refer :all]))

(describe "Keys"

  (it "encoding"
    (should= "YQ" (encode-key "a"))
    (should= "YWI" (encode-key "ab"))
    (should= "YWJj" (encode-key "abc"))
    (should= "YWJjZA" (encode-key "abcd"))
    (should= "YWJjZGU" (encode-key "abcde"))
    (should= "YWJjZGVm" (encode-key "abcdef")))

  (it "decoding"
    (should= "a" (decode-key "YQ"))
    (should= "ab" (decode-key "YWI"))
    (should= "abc" (decode-key "YWJj"))
    (should= "abcd" (decode-key "YWJjZA"))
    (should= "abcde" (decode-key "YWJjZGU"))
    (should= "abcdef" (decode-key "YWJjZGVm")))

  (it "composes and decomposes a large key"
    (let [kind "testing"
          id "BLODQF0Z1DMEfQr7S3eBwfsX4ku"
          key (compose-key kind id)]
      (should= [kind, id] (decompose-key key))))

  (it "composes unique keys"
    (should= 100 (count (into #{} (take 100 (repeatedly #(compose-key "foo"))))))
    (should= 100 (count (into #{} (take 100 (repeatedly #(compose-key "bar"))))))
    (should= (compose-key "foo" 123) (compose-key "foo" 123)))

  (it "decomposes keys"
    (let [key (compose-key "foo" "abc123")]
      (should= ["foo" "abc123"] (decompose-key key))))

  (it "decomposes keys containing a :"
    (let [key (compose-key "foo" "abc:123")]
      (should= ["foo" "abc:123"] (decompose-key key))))

  (it "decomposes keys kind as a keyword"
    (let [key (compose-key :foo "123")]
      (should= ["foo" "123"] (decompose-key key))))

  (it "random-fodder-seq"
    (should= true (seq? (random-fodder-seq)))
    (should= 10 (count (set (map (fn [_] (take 5 (random-fodder-seq))) (range 10))))))

  (it "random-fodder-seq is threadsafe"
     (should= 250
        (reduce +
                (map
                  (fn[_] (count (set (pmap
                                       (fn [_] (take 10 (random-fodder-seq)))
                                       (range 50)))))
                  (range 5)))))

;  (it "generate key times"
;    (prn (take 100 (iterate (fn [_] (generate-id)) nil)))
;    (prn (take 100 (iterate (fn [_] (generate-id2)) nil)))
;    (time (take 1000 (iterate (fn [_] (generate-id)) nil)))
;    (time (take 1000 (iterate (fn [_] (generate-id2)) nil)))
;    )

  )

(run-specs)
