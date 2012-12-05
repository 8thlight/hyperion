# 3.5.1

* fixes equality filters on nil values

# 3.5.0

* fixes issue where saving entity with reference key in sql failed. (https://github.com/8thlight/hyperion/pull/15)
* simplifies key composing resulting in shorter keys.
* adds support for equality filters on nil values (mostly SQL)
* refactors field formatting out of the core api into sql datastore
* adds the ability to alias the field name for the db
* adds first pass of logging using timbre

# 3.4.0

* fixes :key type by introducing the hyperion.types/foreign-key
* refactors transactions to support nesting

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

