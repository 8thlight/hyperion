hyperion.postgres
============

A Hyperion implementation for PostgreSQL

## Types

### Natively Supported

* `java.lang.Boolean` as column type `BOOLEAN`
* `java.lang.Integer` as column type `INTEGER`
* `java.lang.Long` as column type `BIGINT`
* `java.lang.Double` as column type `FLOAT`

### Supported by Packer/Unpacker

* `java.lang.Float` as column type `FLOAT` (will unpack to a double by default)

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.

