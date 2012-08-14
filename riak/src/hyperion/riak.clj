(ns hyperion.riak
  (:require [hyperion.core :refer (Datastore)]
            [chee.util :refer (->options)]
            [cheshire.core :refer (generate-string parse-string)])
  (:import [com.basho.riak.client.builders RiakObjectBuilder]
           [com.basho.riak.client.query.functions NamedErlangFunction JSSourceFunction]
           [com.basho.riak.client.query.indexes BinIndex]
           [com.basho.riak.client.query IndexMapReduce]
           [com.basho.riak.client.raw.http HTTPClientConfig$Builder HTTPRiakClientFactory]
           [com.basho.riak.client.raw.pbc PBClientConfig$Builder PBRiakClientFactory]
           [com.basho.riak.client.raw.query.indexes BinValueQuery]
           [com.basho.riak.client.raw RawClient]
           [com.basho.riak.client.raw StoreMeta StoreMeta$Builder]
           ))

(defn pbc-config [options]
  (let [^PBClientConfig$Builder config (PBClientConfig$Builder.)]
    (when (:host options) (.withHost config (:host options)))
    (when (:port options) (.withPort config (:port options)))
    (when (:connection-timeout-millis options) (.withConnectionTimeoutMillis config (:connection-timeout-millis options)))
    (when (:idle-connection-ttl-millis options) (.withIdleConnectionTTLMillis config (:idle-connection-ttl-millis options)))
    (when (:initial-pool-size options) (.withInitialPoolSize config (:initial-pool-size options)))
    (when (:pool-size options) (.withPoolSize config (:pool-size options)))
    (when (:socket-buffer-size-kb options) (.withSocketBufferSizeKb config (:socket-buffer-size-kb options)))
    (.build config)))

(defn http-config [options]
  (let [^HTTPClientConfig$Builder config (HTTPClientConfig$Builder.)]
    (when (:host options) (.withHost config (:host options)))
    (when (:port options) (.withPort config (:port options)))
    (when (:http-client options) (.withHttpClient config (:http-client options)))
    (when (:mapreduce-path options) (.withMapreducePath config (:mapreduce-path options)))
    (when (:max-connections options) (.withMaxConnctions config (:max-connections options))) ; typo intended!
    (when (:retry-handler options) (.withRetryHandler config (:retry-handler options)))
    (when (:riak-path options) (.withRiakPath config (:riak-path options)))
    (when (:scheme options) (.withScheme config (:scheme options)))
    (when (:timeout options) (.withTimeout config (:timeout options)))
    (when (:url options) (.withUrl config (:url options)))
    (.build config)))

(defn build-connection-config [options]
  (case (.toLowerCase (name (:api options)))
    "pbc" (pbc-config options)
    "http" (http-config options)
    (throw (Exception. (str "Unrecognized Riak API: " (:api options))))))

(defn open-client [& args]
  (let [options (->options args)
        config (build-connection-config options)]
    (case (.toLowerCase (name (:api options)))
      "pbc" (.newClient (PBRiakClientFactory/getInstance) config)
      "http" (.newClient (HTTPRiakClientFactory/getInstance) config))))

(declare ^String ^:dynamic *bucket*)

(def ^StoreMeta store-options
  (-> (StoreMeta$Builder.)
    (.returnBody true)
    (.build)))

; MDM Although Riak will happy generate keys for us,
; the Java client doesn't support this. Grrrrrr!
; They plan to implement it in the Java client so remove when possible
; http://stackoverflow.com/questions/11781857/auto-generated-key-in-riak-via-java-client
(defn- generate-key []
  (.replace (str (java.util.UUID/randomUUID)) "-" ""))

; TODO - investigate using SMILE format of JSON to be faster.
(defn- save-record [client record]
  (let [key (or (:key record) (generate-key))
        json (generate-string (assoc record :key key))
        builder
        (->
          (RiakObjectBuilder/newBuilder *bucket* key)
          (.withValue json)
          (.addIndex "kind" (:kind record)))
        native (.build builder)
        response (.store client native store-options)
        native-result (first (.getRiakObjects response))
        saved-json (.getValueAsString native-result)]
    (parse-string saved-json)))

(defn- find-by-kind [client kind filters sorts limit offset]
  (let [index (BinIndex/named "kind")
        query (BinValueQuery. index *bucket* kind)
        map-reduce (-> (IndexMapReduce. client query) (.addMapPhase NamedErlangFunction/MAP_OBJECT_VALUE))
        json-objects (.getResult (.execute map-reduce) Object)]
    (map parse-string json-objects)))

(deftype RiakDatastore [^RawClient client]
  Datastore
  (ds-save [this records] (doall (map #(save-record client %) records)))
  (ds-delete-by-kind [this kind filters])
  (ds-delete-by-id [this kind id])
  (ds-count-by-kind [this kind filters])
  (ds-find-by-id [this kind id])
  (ds-find-by-kind [this kind filters sorts limit offset] (find-by-kind client kind filters sorts limit offset))
  (ds-all-kinds [this]))

;(ds-save [this natives] (doall (map #(save-native service %) natives)))
;(ds-delete-by-kind [this kind filters]
;  (delete-by-kind service kind filters))
;(ds-delete-by-id [this kind id]
;  (.delete service [(->key id)]))
;(ds-count-by-kind [this kind filters] (count-by-kind service kind filters))
;(ds-find-by-id [this kind id]
;  (find-by-key service id))
;(ds-find-by-kind [this kind filters sorts limit offset]
;  (find-by-kind service kind filters sorts limit offset))
;(ds-all-kinds [this] (all-kinds service)))

(defn new-riak-datastore [client]
  (RiakDatastore. client))