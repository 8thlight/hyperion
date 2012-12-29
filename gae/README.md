hyperion.gae
============

A Hyperion implementation for the Google App Engine datastore

## Types

### Natively Supported

* `java.lang.Boolean`
* `java.lang.Long`
* `java.lang.Double`

### Supported by Packer/Unpacker

* `java.lang.Byte` (if type is not specified, GAE unpacks `Byte`s as `Integer`s)
* `java.lang.Integer` (if type is not specified, GAE unpacks `Integer`s as `Long`s)
* `java.lang.BigInteger` (stored as a binary string because `Blob`s aren't comparable)
* `java.lang.Float` (if type is not specified, GAE unpacks `Float`s as `Double`s)

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.
