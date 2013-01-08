hyperion.gae
============

A Hyperion implementation for the Google App Engine datastore

## Types

### Natively Supported

* `java.lang.Boolean`
* `java.lang.Long`
* `java.lang.Double`
* `java.lang.String`

### Supported by Packer/Unpacker

* `java.lang.Byte` (if type is not specified, GAE unpacks `Byte`s as `Integer`s)
* `java.lang.Short` (if type is not specified, GAE unpacks `Short`s as `Long`s)
* `java.lang.Integer` (if type is not specified, GAE unpacks `Integer`s as `Long`s)
* `java.lang.Float` (if type is not specified, GAE unpacks `Float`s as `Double`s)
* `java.lang.Character`
* `clojure.lang.Keyword`

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.
