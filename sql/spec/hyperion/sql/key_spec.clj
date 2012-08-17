(ns hyperion.sql.key-spec
  (:require [speclj.core :refer :all ]
            [hyperion.sql.key :refer :all ]
            [clojure.data.codec.base64 :refer [encode decode]]))

(describe "Hyperion SQL Key"

  (it "creates keys based on kind and id"
    (should= (create-key "foo" 1) (create-key "foo" 1))
    (should-not= (create-key "foo" 1) (create-key "foo" 2))
    (should-not= (create-key "foo" 1) (create-key "bar" 1)))

  (it "can parse a key"
    (let [key (String. (encode (.getBytes (str "foo:123"))))]
      (should= ["foo" 123] (decompose-key key))))

  )
