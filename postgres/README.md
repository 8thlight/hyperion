hyperion.postgres
============

A Hyperion implementation for PostgreSQL

## Types

### Natively Supported

* `java.lang.Boolean` as column type `BOOLEAN`
* `java.lang.Integer` as column type `INTEGER`
* `java.lang.Long` as column type `BIGINT`
* `java.lang.Double` as column type `FLOAT`
* `java.lang.String` as column type `VARCHAR`

### Supported by Packer/Unpacker

* `java.lang.Byte` as column type `SMALLINT` (will unpack to an `Integer` by default)
* `java.lang.Float` as column type `FLOAT` (will unpack to a `Double` by default)
* `clojure.lang.Keyword` same as `java.lang.String`

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.

