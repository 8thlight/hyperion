(ns hyperion.log-spec
  (:require [speclj.core :refer :all ]
            [hyperion.log :refer :all ]))

(deftype FakeLogger [received level]
  Logger
  (log! [this level message]
    (swap! received conj {:level level :message message}))
  (set-level [this level-to-set]
    (reset! level level-to-set)))

(defn new-fake-logger []
  (FakeLogger. (atom []) (atom nil)))

(describe "Log"

;  (it "default logger exists"
;    (should-not= nil *logger*)
;    (should= java.util.logging.Logger (class *logger*))
;    (should= "hyperion" (.getName *logger*))
;    (should= java.util.logging.Level/WARNING (.getLevel *logger*)))
;
;  (context "to string"
;
;    (with stream (java.io.ByteArrayOutputStream.))
;    (before (remove-all-handlers!))
;
;    (it "logs are formatted"
;      (let [handler (java.util.logging.StreamHandler. @stream FORMATTER)]
;        (add-handler! handler)
;        (warn "foo")
;        (.flush handler)
;        (should= "WARNING blah" (String. (.toByteArray @stream)))))
;
;    )
;
;  (it "doesn't crash when logging"
;    (should-not-throw (debug "foo")))
;
;  (context "with fake logger"
;    (with fake-logger (new-fake-logger))
;    (around [it]
;      (binding [*logger* @fake-logger]
;        (it)))
;
;    (it "logs info statements"
;      (info "My information")
;      (should= "My information" (:message (first @(.received @fake-logger))))
;      (should= java.util.logging.Level/INFO (:level (first @(.received @fake-logger)))))
;
;    (it "logged objects turn into strings"
;      (info "foo:" 1 2 3)
;      (should= "foo: 1 2 3" (:message (first @(.received @fake-logger)))))
;
;    (it "logs warnings"
;      (warn "warning!" 1)
;      (should= "warning! 1" (:message (first @(.received @fake-logger))))
;      (should= java.util.logging.Level/WARNING (:level (first @(.received @fake-logger)))))
;
;    (it "logs debug"
;      (should (.isInstance java.util.logging.Level DEBUG))
;      (should= "DEBUG" (.getName DEBUG))
;      (should= (- (.intValue java.util.logging.Level/CONFIG) 50) (.intValue DEBUG))
;      (debug "debug!" 1)
;      (should= "debug! 1" (:message (first @(.received @fake-logger))))
;      (should= DEBUG (:level (first @(.received @fake-logger)))))
;
;    (it "logs error"
;      (should (.isInstance java.util.logging.Level ERROR))
;      (should= "ERROR" (.getName ERROR))
;      (should= (.intValue java.util.logging.Level/SEVERE) (.intValue ERROR))
;      (error "error!" 1)
;      (should= "error! 1" (:message (first @(.received @fake-logger))))
;      (should= ERROR (:level (first @(.received @fake-logger)))))
;
;    (it "sets the level"
;      (warn-on!)
;      (should= java.util.logging.Level/WARNING @(.level @fake-logger))
;      (info-on!)
;      (should= java.util.logging.Level/INFO @(.level @fake-logger))
;      (debug-on!)
;      (should= DEBUG @(.level @fake-logger)))
;
;    (it "turns to logger off!"
;      (off!)
;      (should= java.util.logging.Level/OFF @(.level @fake-logger)))
;
;    )

  )