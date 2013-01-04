hyperion.mysql
============

A Hyperion implementation for MySQL

## Types

### Natively Supported

* `java.lang.Boolean` as column type `BOOLEAN`
* `java.lang.Integer` as column type `INTEGER`
* `java.lang.Long` as column type `BIGINT`
* `java.lang.Double` as column type `DOUBLE`
* `java.lang.String` as column type `VARCHAR`

### Supported by Packer/Unpacker

* `java.lang.Byte` as column type `TINYINT`
* `java.lang.Float` as column type `DOUBLE` (`FLOAT` will truncate)

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.

