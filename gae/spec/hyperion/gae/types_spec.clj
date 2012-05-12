(ns hyperion.gae.types-spec
  (:use
    [speclj.core]
    [hyperion.gae.types]))

(describe "GAE Types"

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