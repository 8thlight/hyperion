hyperion.riak
============

A Hyperion implementation for Riak

## About

 1. Records are stored as JSON in buckets that correspond to their :kind.
 2. Buckets are namespaced with the value of *app* as a prefix to the bucket name.
    ie. Given that *app* is bound to the value \"my_app_\", a record of kind \"widget\"
    will be stored in the \"my_app_widget\" bucket.
 3. All buckets are implicitly created with default options.  Siblings should not occur.
 4. All fields of each record are indexed to optimize searching.
 5. all "-by-kind" queries execute a MapReduce query which will handle all filtering, sorting, limiting, and offseting.
    According to the [Riak documentation](http://docs.basho.com/riak/1.1.0/tutorials/querying/MapReduce/),
    MapReduce queries should not be performed across an entire bucket (a.k.a kind).
    In order to avoid this, you should use one of the folling optimized filters when performing a "-by-kind" query.
    * one `:=`. This is optimized to execute an equals query on the secondary index.
    * `:<` and `:>` together. This is optimized to execute a range query on the secondary index.
    * one `:>=`. This is optimized to execute a range query on the secondary index.
    * one `:<=`. This is optimized to execute a range query on the secondary index.

    All other operations on the "-by-kind" queries should be avoided.
    They will perform poorly and place a large demand on your cluster.

## Usage

### Opening a Riak client

```clojure
(new-riak-datastore [options])
```

i.e.

```clojure
(new-riak-datastore :api :pbc)
```

Options:

 * `:api` - [`:pbc` `:http`] *required

HTTP Options:

 * `:host`
 * `:port`
 * `:http-client`
 * `:mapreduce-path`
 * `:max-connections`
 * `:retry-handler`
 * `:riak-path`
 * `:scheme`
 * `:timeout`
 * `:url`

 See the [HTTPClientConfig](http://basho.github.com/riak-java-client/1.0.5/com/basho/riak/client/raw/http/HTTPClientConfig.Builder.html) for more info.

PBC Options:

 * `:host`
 * `:port`
 * `:connection-timeout-millis`
 * `:idle-connection-ttl-millis`
 * `:initial-pool-size`
 * `:pool-size`
 * `:socket-buffer-size-kb`

 See the [PBClientConfig](http://basho.github.com/riak-java-client/1.0.5/com/basho/riak/client/raw/pbc/PBClientConfig.Builder.html) for more info.

### Riak Configuration

Riak's configuration is located at (when installed using homebrew): /usr/local/Cellar/riak/1.1.4-x86_64/libexec/etc/app.config
Hyperion Riak requires LeveDB persistence.  Change the config file storage portion like so:

```erlang
%% Storage_backend specifies the Erlang module defining the storage
%% mechanism that will be used on this node.
%% {storage_backend, riak_kv_bitcask_backend},
{storage_backend, riak_kv_eleveldb_backend},
```

## Development

### Configuration

The specs require that riak's delete_mode is immediate.  Add the following to the riak_kv section of config.

```erlang
{delete_mode, immediate}
```

For more info: http://lists.basho.com/pipermail/riak-users_lists.basho.com/2011-October/006048.html

## Types

### Natively Supported

* `java.lang.Boolean`

### Supported by Packer/Unpacker

* `java.lang.Byte`
* `java.lang.Integer`
* `java.lang.Long`
* `java.lang.Float`
* `java.lang.Double`

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.
