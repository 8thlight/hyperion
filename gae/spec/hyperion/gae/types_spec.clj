(ns hyperion.gae.types-spec
  (:require [speclj.core :refer :all]
            [hyperion.gae.types :refer :all]
            [hyperion.api :refer [pack unpack]]))

(describe "GAE Types"

  (context "bytes"
    (it "packs to a byte"
      (let [packed (pack Byte (int 1))]
        (should= 1 packed)
        (should= Byte (type packed))))

    (it "unpacks to a byte"
      (let [unpacked (unpack Byte (int 1))]
        (should= 1 unpacked)
        (should= Byte (type unpacked))))

    )

  (context "integers"
    (it "packs an integer"
      (should= (int 1) (pack Integer (long 1)))
      (should= Integer (type (pack Integer (long 1)))))

    (it "unpacks a Integer"
      (should= (int 1) (unpack Integer (long 1)))
      (should= Integer (type (unpack Integer (long 1)))))

    )

  (context "floats"
    (it "packs a float"
      (should= (float 1.0) (pack Float (double 1.0)))
      (should= Float (type (pack Float (double 1.0)))))

    (it "unpacks a float"
      (should= (float 1.0) (unpack Float (double 1.0)))
      (should= Float (type (unpack Float (double 1.0)))))

    )

  (context "bigintegers"
    (it "packs to a binary string"
      (should= "10" (pack BigInteger (BigInteger. "2")))
      (should= "10" (pack BigInteger (byte 2)))
      (should= "10" (pack BigInteger (short 2)))
      (should= "10" (pack BigInteger (int 2)))
      (should= "10" (pack BigInteger (long 2))))

    (it "unpacks to a big int"
      (should= (BigInteger. "2") (unpack BigInteger "10"))
      (should= (BigInteger. "2") (unpack BigInteger (BigInteger. "2"))))

    )

  (context "User conversion"

    (it "creates user from a full map"
      (let [user (map->user {:auth-domain "yahoo.com"
                             :email "joe@yahoo.com"
                             :federated-identity "http://yahoo.com/joe"
                             :nickname "joe"
                             :user-id "1234567890"})]
        (should= "yahoo.com" (.getAuthDomain user))
        (should= "joe@yahoo.com" (.getEmail user))
        (should= "http://yahoo.com/joe" (.getFederatedIdentity user))
        (should= "joe" (.getNickname user))
        (should= "1234567890" (.getUserId user))))

    (it "creates user from a minimal map"
      (let [user (map->user {:auth-domain "yahoo.com"
                             :email "joe@yahoo.com"})]
        (should= "yahoo.com" (.getAuthDomain user))
        (should= "joe" (.getNickname user))
        (should= "joe@yahoo.com" (.getEmail user))))

    (it "creates user from a medium map"
      (let [user (map->user {:auth-domain "yahoo.com"
                             :email "joe@yahoo.com"
                             :user-id "1234567890"})]
        (should= "yahoo.com" (.getAuthDomain user))
        (should= "joe@yahoo.com" (.getEmail user))
        (should= "joe" (.getNickname user))
        (should= "1234567890" (.getUserId user))))

    (it "creates map from a user"
      (let [values {:auth-domain "yahoo.com"
                    :email "joe@yahoo.com"
                    :federated-identity "http://yahoo.com/joe"
                    :nickname "joe"
                    :user-id "1234567890"}
            user (map->user values)
            result (user->map user)]
        (should= values result)))
    )
  )
