hyperion.redis
============

A Hyperion implementation for Redis

## About

Redis can by used with Hyperion for quick and dirty key value stores.
Currently all filtering happens in Clojure itself and is very slow. If your
application uses filtering very much, you may want to avoid Redis as your
Hyperion datastore.

## Usage

```clojure
(require 'hyperion.api)
(require 'hyperion.redis)
(binding [hyperion.api/*ds* (hyperion.redis/new-redis-datastore :host "localhost" :port 6379)]
    (save {:kind "test" :value "test"})
    (find-by-kind "test"))
```

### new-redis-datastore options

 * :host - where the redis-server is running
 * :port - same as above
 * :password - if the redis-server has one
 * :timeout - defaults to 300 seconds
 * :db - if you are running multiple dbs on a redis-server

## Types

### Natively Supported

* `java.lang.Boolean`
* `java.lang.Integer`

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.
