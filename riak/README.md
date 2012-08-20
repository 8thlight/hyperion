# riak

A Hyperion implementation for Riak

## Usage

### Configuration

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

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.
