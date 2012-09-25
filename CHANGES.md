# 3.4.0

* adds support for nested types
* fixes abstract :key type

# 3.3.0

* renames hyperion.core to hyperion.api

# 3.2.0

* removes DS atom
* adds set-ds! to globally install a datastore

# 3.1.0

* adds hyperion.core/new-datastore factory function
* all implementations have more flexible constructors

# 3.0.0

* adds Riak
* adds Mongo
* uses find-by-key instead of find-by-id.  (same with delete)
* adds abstract :key type in defentity so that relationships are portable

