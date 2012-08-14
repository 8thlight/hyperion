(ns hyperion.riak-spec
  (:require [speclj.core :refer :all ]
            [hyperion.core :refer :all ]
            [hyperion.dev.spec :refer [it-behaves-like-a-datastore]]
            [hyperion.riak.spec-helper :refer [with-testable-riak-datastore]]
            [hyperion.riak :refer :all]))

(describe "Riak Datastore"

  (context "Connection Config"

    (it "configures PBC connection"
      (let [config-map {:api :pbc
                        :host "foo.bar.com"
                        :port 123
                        :connection-timeout-millis 234
                        :idle-connection-ttl-millis 345
                        :initial-pool-size 5
                        :pool-size 6
                        :socket-buffer-size-kb 456}
            config (build-connection-config config-map)]
        (should= "com.basho.riak.client.raw.pbc.PBClientConfig" (.getName (class config)))
        (should= "foo.bar.com" (.getHost config))
        (should= 123 (.getPort config))
        (should= 234 (.getConnectionWaitTimeoutMillis config))
        (should= 345 (.getIdleConnectionTTLMillis config))
        (should= 5 (.getInitialPoolSize config))
        (should= 6 (.getPoolSize config))
        (should= 456 (.getSocketBufferSizeKb config))))

    (it "configures HTTP connection"
      (let [http-client (org.apache.http.impl.client.DefaultHttpClient.)
            retry-handler (org.apache.http.impl.client.DefaultHttpRequestRetryHandler.)
            config-map {:api :http
                        :host "foo.bar.com"
                        :port 123
                        :http-client http-client
                        :mapreduce-path "map/reduce"
                        :max-connections 234
                        :retry-handler retry-handler
                        :riak-path "riak/path"
                        :scheme "https"
                        :timeout 345}
            config (build-connection-config config-map)]
        (should= "com.basho.riak.client.raw.http.HTTPClientConfig" (.getName (class config)))
        (should= "https://foo.bar.com:123/riak/path" (.getUrl config))
        (should= http-client (.getHttpClient config))
        (should= "map/reduce" (.getMapreducePath config))
        (should= 234 (.getMaxConnections config))
        (should= retry-handler (.getRetryHandler config))
        (should= 345 (.getTimeout config))))


    (it "configures HTTP connection using :url option"
      (let [config-map {:api :http
                        :url "https://foo.bar.com:123/riak/path"}
            config (build-connection-config config-map)]
        (should= "com.basho.riak.client.raw.http.HTTPClientConfig" (.getName (class config)))
        (should= "https://foo.bar.com:123/riak/path" (.getUrl config))))

    )

  (context "PBC client"

    (with client (open-client :api :pbc))
    (after (try (.shutdown @client) (catch Exception e)))

    (it "creating a PBC client"
      (should= "com.basho.riak.client.raw.pbc.PBClientAdapter" (.getName (class @client)))
      (should-not-throw (.ping @client)))
    )

  (context "HTTP client"

    (with client (open-client :api :http))
    (after (try (.shutdown @client) (catch Exception e)))

    (it "creating an HTTP client"
      (should= "com.basho.riak.client.raw.http.HTTPClientAdapter" (.getName (class @client)))
      (should-not-throw (.ping @client)))
    )

  (context "Live"
    (with-testable-riak-datastore)

;    (it-behaves-like-a-datastore)

    (it "it saves an existing record"
      (let [record1 (save {:kind "other-testing" :name "ann"})
            record2 (save (assoc record1 :name "james"))]
        (should= (:id record1) (:id record2))
        (should= 1 (count (find-by-kind "other-testing")))))
    )
  )


