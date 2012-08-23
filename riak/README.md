# riak

A Hyperion implementation for Riak


## About

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

## Usage

### Opening a Riak client

    (new-riak-datastore [options])

i.e.

    (new-riak-datastore :api :pbc)

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

 See: http://basho.github.com/riak-java-client/1.0.5/com/basho/riak/client/raw/pbc/PBClientConfig.Builder.html

### Riak Configuration

Riak's configuration is located at (when installed using homebrew): /usr/local/Cellar/riak/1.1.4-x86_64/libexec/etc/app.config
Hyperion Riak requires LeveDB persistence.  Change the config file storage portion like so:

    %% Storage_backend specifies the Erlang module defining the storage
    %% mechanism that will be used on this node.
    %% {storage_backend, riak_kv_bitcask_backend},
    {storage_backend, riak_kv_eleveldb_backend},

## Development

### Configuration

The specs require that riak's delete_mode is immediate.  Add the following to the riak_kv section of config.

    {delete_mode, immediate}

For more info: http://lists.basho.com/pipermail/riak-users_lists.basho.com/2011-October/006048.html

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.
