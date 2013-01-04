hyperion.mongo
============

A Hyperion implementation for Mongo

## About

Mongo fits rather well with the Hyperion model.  The API is 100% supported with little to no inefficiencies.
Indexes should be added externally since Hyperion doesn't support indexes.

## Usage

```clojure
(require 'hyperion.api)
(require 'hyperion.mongo)
(binding [hyperion.api/*ds* (hyperion.mongo/new-mongo-datastore :host "localhost" :port 27017 :database "mydb")]
    (save {:kind "test" :value "test"})
    (find-by-kind "test"))
```

### Creating a datastore piecemeal

```clojure
(use 'hyperion.mongo)
(def mongo (open-mongo :host "localhost"))
(def database (open-database mongo "mydb"))
(def datastore (new-mongo-datastore database))
```

### open-mongo options

 * :host - specifies the host, presumes a single connection
 * :port - defaults to 27017, presumes a single connection
 * :servers - a sequence of [host, port] pairs, for connecting to multiple servers
 * :ssl - defaults to false, for Mongo servers configured for SSL.  Use the value :trust if you want to bypass CA verification.


### open-database options

 * :username - for severs configured to require credentials
 * :password - same as above
 * :write-concern - http://api.mongodb.org/java/2.8.0/com/mongodb/WriteConcern.html
    * :fsync-safe - Exceptions are raised for network issues, and server errors; the write operation waits for the server to flush the data to disk
    * :journal-safe - Exceptions are raised for network issues, and server errors; the write operation waits for the server to group commit to the journal file on disk
    * :majority - Exceptions are raised for network issues, and server errors; waits on a majority of servers for the write operation
    * :none - No exceptions are raised, even for network issues
    * :normal - Exceptions are raised for network issues, but not server errors
    * :replicas-safe - Exceptions are raised for network issues, and server errors; waits for at least 2 servers for the write operation
    * :safe - DEFAULT - Exceptions are raised for network issues, and server errors; waits on a server for the write operation

## Types

### Natively Supported

* `java.lang.Boolean`
* `java.lang.Integer`
* `java.lang.Long`
* `java.lang.Double`
* `java.lang.String`

### Supported by Packer/Unpacker

* `java.lang.Byte` (if type is not specified, Mongo will unpack to an `Integer`)
* `java.lang.Float` (if type is not specified, Mongo will unpack to a `Double`)
* `clojure.lang.Keyword`

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.
