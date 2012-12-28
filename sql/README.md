hyperion.sql
============

This package encapsulates the common behavior of the SQL datastores:

  * Query building
  * Query executing via [JDBC](http://docs.oracle.com/javase/tutorial/jdbc/basics/index.html)
  * Connections
  * Transactions

The cases where you will have to use the SQL package directly are detailed here.

## Instantiating a datastore

When a SQL datastore is instantiated the connection-url must passed in as a parameter. This is the connection url that the datastore will use when attemting to communicate with the database.

```clojure
(require 'hyperion.api)
(binding [hyperion.api/*ds* (new-datastore :implementation :mysql :connection-url "jdbc:mysql://localhost:3306/myapp?user=root" :database "myapp")]
    (save {:kind "test" :value "test"})
    (find-by-kind "test"))
```

## Connection Pooling

By default, all connections in Hyperion are pooled by connection url.

## Opening a connection manually

Sometimes it may be useful to open a connection manually, like at the start of a web request, or to start a transaction.

``` clojure
(ns my-app.core
  (:require [hyperion.sql.connection :refer [with-connection]]))

(with-connection 'postgres://ispvswov:cwtTUFRBRgnc@ec2-23-23-234-187.compute-1.amazonaws.com:5432/d2nh0jkh0n8j3l'
  ;code that uses a connection
  )
```

Make sure that the connection url passed in here is the same connection url passed into the datastore upon instantiation.

## Transactions

Before you start a transaction, you must have an open connection. This means you will have to open a connection manually (see above) or have one open already.

``` clojure
(use 'hyperion.sql.jdbc :only [transaction])

(transaction
  ; code that requires a transaction
  )
```

If an exception is thrown in a transaction block, the transaction will be rolled back.

Hyperion also supports nested transactions.

``` clojure
(use 'hyperion.api)
(use 'hyperion.sql.jdbc :only [transaction])

(try
  (transaction
    (save :kind :person :name "Myles")
    (count-by-kind :person) ;=> 1
    (try
      (transaction
        (save :kind :person :name "Myles")
        (count-by-kind :person) ;=> 2
        (throw (IllegalStateException.)))
      (catch IllegalStateException _))
    (count-by-kind :person) ;=> 1
    (throw (IllegalStateException.)))
  (catch IllegalStateException _))
(count-by-kind :person) ;=> 0
```

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.

