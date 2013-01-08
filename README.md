<img src="https://raw.github.com/8thlight/hyperion/master/hyperion_logo.png" alt="Hyperion logo" title="Titan of Light" align="right"/>
# Hyperion [![Build Status](https://secure.travis-ci.org/8thlight/hyperion.png)](http://travis-ci.org/8thlight/hyperion)

<em>1 API, multiple database backends.</em>

Hyperion provides you with a simple API for data persistence allowing you to delay the choice of database without delaying your development.

There are a few guiding principles for Hyperion.

 1. key/value store.  All Hyperion implementations, even for relational databases, conform to the simple key/value store API.
 2. values are maps.  Every 'value' that goes in or out of a Hyperion datastore is a map.
 3. :key and :kind.  Every 'value' must have a :kind entry; a short string like "user" or "product".  Persisted 'value's will have a :key entry; strings generated by the datastore.
 4. Search with data.  All searches are described by data.  See find-by-kind below.

Hyperion Implementations:

 * [memory](https://github.com/8thlight/hyperion/blob/master/api/src/hyperion/memory.clj) - an in-memory datastore, ideal for testing, included in hyperion-api package
 * [gae](https://github.com/8thlight/hyperion/tree/master/gae) - [Google App Engine Datastore](https://developers.google.com/appengine/docs/python/datastore/overview)
 * [mongo](https://github.com/8thlight/hyperion/tree/master/mongo) - [Mongo DB](http://www.mongodb.org/)
 * [mysql](https://github.com/8thlight/hyperion/tree/master/mysql) - [MySQL](http://www.mysql.com/)
 * [postgres](https://github.com/8thlight/hyperion/tree/master/postgres) - [PostgreSQL](http://www.postgresql.org/)
 * [riak](https://github.com/8thlight/hyperion/tree/master/riak) - [Riak](http://wiki.basho.com/Riak.html)
 * [sqlite](https://github.com/8thlight/hyperion/tree/master/sqlite) - [SQLite](http://www.sqlite.org/)
 * [redis](https://github.com/8thlight/hyperion/tree/master/redis) - [Redis](http://redis.io/)

## Installation

### Leiningen

```clojure
:dependencies [[hyperion/hyperion-<impl here> "3.7.0"]]
```

## Usage

### Creating a datastore

hyperion.api provides a convenient factory function for instantiating any datastore implementation

```clojure
(use 'hyperion.api)
(new-datastore :implementation :memory)
(new-datastore :implementation :mysql :connection-url "jdbc:mysql://localhost:3306/myapp?user=root" :database "myapp")
(new-datastore :implementation :mongo :host "localhost" :port 27017 :database "myapp" :username "test" :password "test")
```

Each implementation provides their own facilities of course:

```clojure
(use 'hyperion.mongo)
(new-mongo-datastore :host "localhost" :port 27017 :database "myapp" :username "test" :password "test")
;or
(let [mongo (open-mongo :host "127.0.0.1" :port 27017)
      db (open-database mongo "myapp" :username "test" :password "test")]
   (new-mongo-datastore db))
```

### Installing a datastore

```clojure
; with brute force
(set-ds! (new-datastore ...))
; with elegance
(binding [*ds* (new-datastore ...)]
  ; persistence stuff here)
```

Ideally, bind the datastore once at a high level in your application, if you can. Otherwise use the brute force `set-ds!` technique.

### Saving a value:

```clojure
(save {:kind :foo})
;=> {:kind "foo" :key "generated key"}
(save {:kind :foo} {:value :bar})
;=> {:kind "foo" :value :bar :key "generated key"}
(save {:kind :foo} :value :bar)
;=> {:kind "foo" :value :bar :key "generated key"}
(save {:kind :foo} {:value :bar} :another :fizz)
;=> {:kind "foo" :value :bar :another :fizz :key "generated key"}
(save (citizen) :name "Joe" :age 21 :country "France")
;=> #<{:kind "citizen" :name "Joe" :age 21 :country "France" ...}>
```

### Updating a value:

```clojure
(let [record (save {:kind :foo :name "Sue"})
      new-record (assoc record :name "John")]
  (save new-record))
;=> {:kind "foo" :name "John" :key "generated key"}
```

### Loading a value:

```clojure
; if you have a key...
(find-by-key my-key)

; otherwise
(find-by-kind :dog) ; returns all records with :kind of "dog"
(find-by-kind :dog :filters [:= :name "Fido"]) ; returns all dogs whos name is Fido
(find-by-kind :dog :filters [[:> :age 2][:< :age 5]]) ; returns all dogs between the age of 2 and 5 (exclusive)
(find-by-kind :dog :sorts [:name :asc]) ; returns all dogs in alphebetical order of their name
(find-by-kind :dog :sorts [[:age :desc][:name :asc]]) ; returns all dogs ordered from oldest to youngest, and dogs of the same age ordered by name
(find-by-kind :dog :limit 10) ; returns upto 10 dogs in undefined order
(find-by-kind :dog :sorts [:name :asc] :limit 10) ; returns up to the first 10 dogs in alphebetical order of their name
(find-by-kind :dog :sorts [:name :asc] :limit 10 :offset 10) ; returns the second set of 10 dogs in alphebetical order of their name
```

### Deleting a value:

```clojure
; if you have a key...
(delete-by-key my-key)

; otherwise
(delete-by-kind :dog) ; deletes all records with :kind of "dog"
(delete-by-kind :dog :filters [:= :name "Fido"]) ; deletes all dogs whos name is Fido
(delete-by-kind :dog :filters [[:> :age 2][:< :age 5]]) ; deletes all dogs between the age of 2 and 5 (exclusive)
```

### Filters and Sorts

Filter operations and acceptable syntax:

  * `:=` `"="` `"eq"`
  * `:<` `"<"` `"lt"`
  * `:<=` `"<="` `"lte"`
  * `:>` `">"` `"gt"`
  * `:>=` `">="` `"gte"`
  * `:!=` `"!="` `"not"`
  * `:contains?` `"contains?"` `:contains` `"contains"` `:in?` `"in?"` `:in` `"in"`

Sort orders and acceptable syntax:

 * `:asc` `"asc"` `:ascending` `"ascending"`
 * `:desc` `"desc"` `:descending` `"descending"`

The `:filter` and `:sort` options are usable in `find-by-kind`, `find-by-all-kinds`, and `delete-by-kind`.  The `:limit` option may also be used in the `find-by-` functions.

<em>Note:</em> Filters and Sorts on `:key` are not supported.  Some datastore implementations don't store the `:key` along with the data, so you can't very well filter or sort something that aint there.

### Entities

Used to define entities. An entity is simply an encapsulation of data that is persisted.
The advantage of using entities are:

 * they limit the fields persisted to only what is specified in their definition.
 * default values can be assigned to fields.
 * types, packers, and unpackers can be assigned to fields. Packers
     allow you to manipulate a field (perhaps serialize it) before it
     is persisted. Unpacker conversly manipulate fields when loaded.
     Packers and unpackers may be a fn (which will be excuted) or an
     object used to pivot the pack and unpack multimethods.
     A type (object) is simply a combined packer and unpacker.
 * constructors are provided.

Example:

```clojure
(use 'hyperion.types)

(defentity Citizen
    [name]
    [age :packer ->int] ; ->int is a function defined in your code.
    [gender :unpacker ->string] ; ->string is a customer function too.
    [occupation :type my.ns.Occupation] ; and then we define pack/unpack for my.ns.Occupation
    [spouse-key :type (foreign-key :citizen)] ; :key is a special type that pack string keys into implementation-specific keys
    [country :default "USA"] ; newly created records will use the default if no value is provided
    [created-at] ; populated automaticaly
    [updated-at] ; also populated automatically
    )

(save (citizen :name "John" :age "21" :gender :male :occupation coder :spouse-key "abc123"))

;=> #<{:kind "citizen" :key "some generated key" :country "USA" :created-at #<java.util.Date just-now> :updated-at #<java.util.Date just-now> ...)
```

#### Foreign Keys

In a traditional SQL database, you may have a schema that looks like this:

users:
  * id
  * first_name
  * created_at
  * updated_at

profiles:
  * id
  * user_id
  * created_at
  * updated_at

Since Hyperion presents every underlying datastore as a key-value store, configuring Hyperion to use this schema is a little tricky, but certainly possible.

This is what the coresponding `defentity` notation would be:

``` clojure
(use 'hyperion.api)
(use 'hyperion.types)

(defentity :users
  [first-name]
  [created-at]
  [updated-at]
  )

(defentity :profiles
  [user-key :type (foreign-key :users) :db-name :user-id]
  [created-at]
  [updated-at]
  )

(let [myles (save {:kind :users :first-name "Myles"})
      myles-profile (save {:kind :profiles :user-key (:key myles)})]
; myles => {:key "b26316a0248244bab65c699778897ab9", :created-at #inst "2012-12-05T15:41:23.589-00:00", :updated-at #inst "2012-12-05T15:41:23.589-00:00", :first-name "Myles", :kind "users"}
; myles is stored in the users table as:
; | id | first_name | created_at | updated_at |
; | 1  | Myles      | <time>     | <time>     |

; myles-profile => {:key "7202968b5ecf47aab686990750a3238a", :user-key "b26316a0248244bab65c699778897ab9", :created-at #inst "2012-12-05T15:43:16.529-00:00", :updated-at #inst "2012-12-05T15:43:16.529-00:00", :kind "profiles"}
; myles' profile is stored in the profiles table as:
; | id | user_id | created_at | updated_at |
; | 1  | 1       | <time>     | <time>     |

  (= (find-by-key (:user-key myles-profile)) myles) ;=> true
  )

```

Using the `foreign-key` type, our foreign key references are stored following the conventions of the underlying datastore. In this example, the `user-key` field will be packed as an integer id, as stored in the `user-id` column.

If your schema requires foreign keys, **ALWAYS USE THE FOREIGN KEY TYPE**. If you do not, you will be storing generated keys instead of actual database ids. **DO NOT DO THIS**. If Hyperion changes the way it generates keys, all of your foreign key data will be useless.

#### Types

All hyperion implementations provide built-in support for the following types:

* `java.lang.Boolean`
* `java.lang.Byte`
* `java.lang.Short`
* `java.lang.Integer`
* `java.lang.Long`
* `java.lang.Float`
* `java.lang.Double`
* `java.lang.Character`
* `java.lang.String`
* `clojure.lang.Keyword`

Implementations may either support the type Natively or with a packer/unpacker. If they are natively supported, no configuration is needed. If supported by a packer/unpacker, you must explicitly configure the type. For example:

``` clojure
(defentity :users
  [first-name :type java.lang.String]
  [age :type java.lang.Integer])
```

It is always best to explicitly state the types of all fields, regardless of implementation, so that you don't have to worry about the differences between datastores.

#### Unsupported Types

The following types do not have built-in support:

* `java.math.BigInteger`
* `java.math.BigDecimal`

There are many different opinions on the best way to store these types. We will leave it up to you to store them in the way that you see fit.

## Logging

Many of the Hyperion components will log informative information (more logging has yet to be added).  The default log level is _Info_.
Not much is logged at the info level.  To get more informative log message, turn on the _Debug_ log level.

```clojure
(hyperion.log/debug!)
```

You can also log your own messages.

```clojure
(hyperion.log/debug "This is a debug message")
(hyperion.log/info "Hey, here's some info!")
```

The complete list of log levels (which come from [timbre](https://github.com/ptaoussanis/timbre)) are `[:trace :debug :info :warn :error :fatal :report]`.

## Full API

To learn more, downlaod hyperion.api and load up the REPL.

```clojure
user=> (keys (ns-publics 'hyperion.api))
(delete-by-key save* count-all-kinds save find-by-key reload pack create-entity-with-defaults delete-by-kind defentity *ds*
before-save find-by-kind count-by-kind after-load after-create new-datastore ds unpack create-entity set-ds! find-all-kinds new?)

user=> (doc delete-by-key)
-------------------------
hyperion.api/delete-by-key
([key])
  Removes the record stored with the given key.
  Returns nil no matter what.
```
