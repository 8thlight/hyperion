# Hyperion [![Build Status](https://secure.travis-ci.org/mylesmegyesi/hyperion.png)](http://travis-ci.org/mylesmegyesi/hyperion)

## Installation

*TO DO*

## API

### new? [entity]

Returns true if the entity has not yet been saved

### save [entity]

Saves an entity (hash or object) into the database.

    save {:kind "user" :name "Eric"}

### reload [entity]

Reloads the entity to its state in the DB

### delete-by-kind [kind filters]

Deletes entities of kind from the DB where the filter matches

    delete-by-kind "user" :filters [ [:= :name "Eric"] ]

### count-by-kind [kind filters]

Counts entities of kind from the DB where the filter matches

    count-by-kind "user"

### find-by-kind [kind filters sorts limit offset]

Returns entities of kind where the filters match, sorted, limited, and offset

    find-by-kind "user" :sorts "desc" :limit 10 :offset 3

### find-all-kinds [filters sorts limit offset]

The same as find-by-kinds but searches the DB for all kinds

### count-all-kinds [filters]

The same as count-by-kind but searches the DB for all kinds

## Filters

A filter is a vector of conditions.  A conditions is a vector where the
comparison is the first element, the column to search is the second, and the value 
to match is the third.

    [
      [:= :name "Eric"]
      [:> :age 10]
      [:not :age 25]
      [:in :gender ["male" "female"]]
    ]

### Operators

* "=" or "eq"
* "<" or "lt"
* "<=" or "lte"
* ">" or "gt"
* ">=" or "gte"
* "!=" or "not"
* "contains?" or "contains" or "in?" or "in"
