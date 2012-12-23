1. Logging
  * All queries should be logged and profiled with the debug flag
  * All dangerous actions logged with the warn flag
2. Extend the Datastore spec for packing/unpacking/filtering/sorting on the following types:
  * `byte`
  * `char`
  * `clojure.lang.Keyword`
  * `double`
  * `java.io.InputStream`
  * `java.lang.Character`
  * `java.lang.Number`
  * `java.lang.String`
  * `java.lang.StringBuffer` (in as StringBuffer, out as String)
  * `java.lang.StringBuilder` (in as StringBuilder, out as String)
  * `java.math.AtomicInteger`
  * `java.math.AtomicLong`
  * `java.math.BigDecimal`
  * `java.math.BigInteger`
  * `java.math.Byte`
  * `java.math.Double`
  * `java.math.Long`
  * `java.math.Short`
  * `java.net.Url`
  * `java.util.Date`
  * `long`
  * `null`
  * `short`
3. Whatever Java types cannot be natively supported by the Database should have a packer and unpacker included with the Datastore implementation. Documentation on what is natively supported and what is not should be included as well.
4. Improve Riak Performance
  * Optimize filters for secondary indexes
  * Everything not optimized should be a Map/Reduce query using Javascript
  * Nothing done in-memory
5. Improve Redis Datastore
  * Serialize data so that it is compatible with Hyperion-Ruby (aka not marshalled into bytes as it is currently)
  * Optimize filter/sort/limit/offset operations to use the Redis `SORT` query
  * Whatever cannot be optimized should use Lua scripting
  * Nothing done in-memory
6. [Ragtime](https://github.com/weavejester/ragtime) integration for migrations
7. Indexes:
    * Riak: currently indexes ALL fields.  That's inefficient. Yet it can't search efficiently without index.  If defentity allowed fields to be marked as indexed, would help riak a great deal. Indexes are needed at runtime to:
        - save (to know which indexes to add)
        - find-by-kind (so we know how to search)
    * Mongo: really only need indexes at defentity time to "ensure" they exist.  Indexes are not needed at runtime. This could be build into the future migration system instead of application level configuration.
    * GAE: Indexes are handled externally. Would be useless.
    * SQL: Same as Mongo.
    * Implement indexes as datastore decorators?
8. Website. Ahhh...maybe someday we will get this far down on the list.
