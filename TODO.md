* Logging: For debugging.  For runtime monitoring.  For warning of dangerous actions.
* Connection pooling for Sql implementations
* Better transactions (i.e. use savepoints)
* Website?
* Indexes:
    * Riak: currently indexes ALL fields.  That's inefficient.
    Yet it can't search efficiently without index.  If defentity allowed
    fields to be marked as indexed, would help riak a great deal.
    Indexes are needed at runtime to:
        - save (to know which indexes to add)
        - find-by-kind (so we know how to search)
    * Mongo: really only need indexes at defentity time to "ensure"
    they exist.  Indexes are not needed at runtime.
    * GAE: Indexes are handled externally.  Would be useless.
    * SQL: Same as Mongo I suppose.
    * Implement indexes as datastore decorators?
* Ragtime integration for migrations (https://github.com/weavejester/ragtime)
* Datomic - maybe?

