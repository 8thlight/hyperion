1. Logging
  * All queries should be logged and profiled with the debug flag
  * All dangerous actions logged with the warn flag
2. Extend the Datastore spec for packing/unpacking/filtering/sorting on the following types. Whatever Java types cannot be natively supported by the Database should have a packer and unpacker included with the Datastore implementation. Documentation on what is natively supported and what is not should be included as well.
  * `java.lang.Character`
  * `java.util.Date`
  * `null`
3. Improve Redis Datastore
  * Serialize data so that it is compatible with Hyperion-Ruby (aka not marshalled into bytes as it is currently)
  * Optimize filter/sort/limit/offset operations to use the Redis `SORT` query
  * Whatever cannot be optimized should use Lua scripting
  * Nothing done in-memory
4. Migrations:
    * [Ragtime](https://github.com/weavejester/ragtime) integration?
    * Necessity is the mother of invention. Wait until it's needed to build.
5. Indexes:
    * Riak: currently indexes ALL fields.  That's inefficient. Yet it can't search efficiently without index.  If defentity allowed fields to be marked as indexed, would help riak a great deal. Indexes are needed at runtime to:
        - save (to know which indexes to add)
        - find-by-kind (so we know how to search)
    * Mongo: really only need indexes at defentity time to "ensure" they exist.  Indexes are not needed at runtime. This could be build into the future migration system instead of application level configuration.
    * GAE: Indexes are handled externally. Would be useless.
    * SQL: Same as Mongo.
    * Make indexing part of migrations, like Active Record?
6. Website. Ahhh...maybe someday we will get this far down on the list.
