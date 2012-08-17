(ns hyperion.riak
  (:require [hyperion.core :refer [Datastore]]
            [hyperion.memory :as memory]
            [hyperion.sorting :as sort]
            [hyperion.filtering :as filter]
            [chee.util :refer [->options]]
            [cheshire.core :refer [generate-string parse-string]]
            [clojure.data.codec.base64 :refer [encode decode]]
            [clojure.string :as string]
            [clojure.set :refer [intersection]])
  (:import [com.basho.riak.client.builders RiakObjectBuilder]
           [com.basho.riak.client.query.functions NamedErlangFunction JSSourceFunction]
           [com.basho.riak.client.query.indexes BinIndex KeyIndex IntIndex]
           [com.basho.riak.client.query IndexMapReduce]
           [com.basho.riak.client.raw.http HTTPClientConfig$Builder HTTPRiakClientFactory]
           [com.basho.riak.client.raw.pbc PBClientConfig$Builder PBRiakClientFactory]
           [com.basho.riak.client.raw.query.indexes BinValueQuery BinRangeQuery IntValueQuery IntRangeQuery]
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

(defn open-client
  "Create a Riak client. You may pass in a hashmap and/or
  key-value pairs of configuration options.
  Options:
    :api - [:pbc :http] *required
  HTTP Options:
    :host :port :http-client :mapreduce-path :max-connections
    :retry-handler :riak-path :scheme :timeout :url
    See: http://basho.github.com/riak-java-client/1.0.5/com/basho/riak/client/raw/http/HTTPClientConfig.Builder.html
  PBC Options:
    :host :port :connection-timeout-millis
    :idle-connection-ttl-millis :initial-pool-size
    :pool-size :socket-buffer-size-kb
    See: http://basho.github.com/riak-java-client/1.0.5/com/basho/riak/client/raw/pbc/PBClientConfig.Builder.html"
  [& args]
  (let [options (->options args)
        config (build-connection-config options)]
    (case (.toLowerCase (name (:api options)))
      "pbc" (.newClient (PBRiakClientFactory/getInstance) config)
      "http" (.newClient (HTTPRiakClientFactory/getInstance) config))))

(def ^String ^:dynamic *app* "Hyperion")

(defn bucket-name [kind]
  (str *app* kind))

(def ^StoreMeta store-options
  (-> (StoreMeta$Builder.)
    (.returnBody true)
    (.build)))

(defn- generate-id []
  (.replace (str (java.util.UUID/randomUUID)) "-" ""))

(defn create-key
  ([kind] (create-key kind (generate-id)))
  ([kind id] (String. (encode (.getBytes (str kind ":" id))))))

(defn decompose-key [key]
  (string/split (String. (decode (.getBytes key))) #":"))

(defn- ->native [record kind id]
  (let [record (dissoc record :id :kind )
        json (generate-string record)
        builder (RiakObjectBuilder/newBuilder (bucket-name kind) id)]
    (.withValue builder json)
    (doseq [[k v] record]
      (if (integer? v)
        (.addIndex builder (name k) (int v))
        (.addIndex builder (name k) (str v))))
    (.build builder)))

(defn json->record [json kind key]
  (assoc (parse-string json true)
    :kind kind
    :key key))

(defn native->record [native kind id]
  (let [record (parse-string (String. (.getValue native)) true)]
    (assoc record
      :kind kind
      :key (create-key kind id))))

; TODO - investigate using SMILE format of JSON to be faster.
(defn- save-record [client record]
  (let [key (or (:key record) (create-key (:kind record)))
        [kind id] (decompose-key key)
        native (->native record kind id)
        response (.store client native store-options)
        native-result (first (.getRiakObjects response))
        saved-json (.getValueAsString native-result)]
    (json->record saved-json kind key)))

(defn- find-by-key
  ([client key]
    (let [[kind id] (decompose-key key)]
      (find-by-key client (bucket-name kind) kind id)))
  ([client bucket kind id]
    (let [response (.fetch client bucket id)]
      (when (.hasValue response)
        (when (.hasSiblings response)
          (println "Whao! Siblings! Siblings are not allowed by default.  Someone must have tweaked things! bucket:" bucket " key:" id))
        (native->record (first (.getRiakObjects response)) kind id)))))

(defn- delete-by-key
  ([client key]
    (let [[kind id] (decompose-key key)]
      (delete-by-key client (bucket-name kind) id)))
  ([client bucket id] (.delete client bucket id)))

(defn optimize-filters [filters]
  (reduce
    (fn [[q nq] [operator field value]]
      (case operator
        (:= :<= :>= ) [(conj q [operator field value]) nq]
        :< [(conj q [:<= field value]) (conj nq [:!= field value])]
        :> [(conj q [:>= field value]) (conj nq [:!= field value])]
        (:!= :contains? ) [q (conj nq [operator field value])]))
    [[] []]
    filters))

(defn filter->query [bucket [operator field value]]
  (case [operator (if (integer? value) :int :bin )]
    [:= :int ] (IntValueQuery. (IntIndex/named (name field)) bucket (int value))
    [:<= :int ] (IntRangeQuery. (IntIndex/named (name field)) bucket Integer/MIN_VALUE (int value))
    [:>= :int ] (IntRangeQuery. (IntIndex/named (name field)) bucket (int value) Integer/MAX_VALUE)
    [:= :bin ] (BinValueQuery. (BinIndex/named (name field)) bucket value)
    [:<= :bin ] (BinRangeQuery. (BinIndex/named (name field)) bucket "0" value)
    [:>= :bin ] (BinRangeQuery. (BinIndex/named (name field)) bucket value "zzzzz")
    (throw (Exception. (str "Don't know how to create query from filter: " filter)))))

(defn filters->queries [bucket filters]
  (if (seq filters)
    (map (partial filter->query bucket) filters)
    [(BinRangeQuery. KeyIndex/index bucket "0" "zzzzz")]))

(defn- ids-by-kind [client bucket filters]
  (let [queries (filters->queries bucket filters)
        results (map #(.fetchIndex client %) queries)]
    (reduce #(intersection %1 %2) (map set results))))

(defn- find-by-kind [client kind filters sorts limit offset]
  (let [bucket (bucket-name kind)
        [pre-filters post-filters] (optimize-filters filters)
        filter-fn (memory/build-filter kind post-filters)
        ids (ids-by-kind client bucket pre-filters)
        records (map (partial find-by-key client bucket kind) ids)]
    (->> records
      (filter filter-fn)
      (sort/sort-results sorts)
      (filter/offset-results offset)
      (filter/limit-results limit))))

(defn- delete-by-kind [client kind filters]
  (let [records (find-by-kind client kind filters nil nil nil)
        ids (map #(second (decompose-key (:key %))) records)
        bucket (bucket-name kind)]
    (doseq [id ids] (delete-by-key client bucket id))))

(defn- find-all-kinds [client]
  (let [buckets (.listBuckets client)
        buckets (filter #(.startsWith % *app*) buckets)]
    (map #(.substring % (count *app*)) buckets)))

(deftype RiakDatastore [^RawClient client]
  Datastore
  (ds-save [this records] (doall (map #(save-record client %) records)))
  (ds-delete-by-kind [this kind filters] (delete-by-kind client kind filters))
  (ds-delete-by-key [this key] (delete-by-key client key))
  (ds-count-by-kind [this kind filters] (count (find-by-kind client kind filters nil nil nil)))
  (ds-find-by-key [this key] (find-by-key client key))
  (ds-find-by-kind [this kind filters sorts limit offset] (find-by-kind client kind filters sorts limit offset))
  (ds-all-kinds [this] (find-all-kinds client)))

(defn new-riak-datastore
  "Creates a datastore implementation for Riak.
  There are several noteworthy aspects of this implementation.
  1. Records are stored as JSON in buckets that correspond to their :kind.
  2. Buckets are namespaced with the value of *app* as a prefix to the bucket name.
     ie. Given that *app* is bound to the value \"my_app_\", a record of kind \"widget\"
     will be stored in the \"my_app_widget\" bucket.
  3. All buckets are implicitly created with default options.  Siblings should not occur.
  4. All fields of each record are indexed to optimize searching.
  5. Only certain types of search operation are optimized.  They are [:= :<= :>=].
     Operations [:< :>] are mostly optimized but require some in memory filtering.
     Operations [!= :contains?] may have VERY poor performance because all the records
     of the specified kind will be loaded and filtered in memory.
  6. Sort, Offset, and Limit search options are handled in memory because Riak doesn't
     provide a facility for these.  Expect poor performance."
  [client]
  (RiakDatastore. client))