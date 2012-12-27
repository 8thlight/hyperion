hyperion.gae
============

A Hyperion implementation for the Google App Engine datastore

## Types

### Natively Supported

* `java.lang.Boolean`
* `java.lang.Long`
* `java.lang.Double`

### Supported by Packer/Unpacker

* `java.lang.Integer` (if type is not specified, GAE unpacks integers as longs)
* `java.lang.Float` (if type is not specified, GAE unpacks floats as doubles)

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.
