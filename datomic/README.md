# Datomic

A Hyperion implementation for Datomic

## About

## Usage

    (require 'hyperion.core)
    (require 'hyperion.datomic)
    (binding [hyperion.core/*ds* (hyperion.datomic/new-datomic-datastore ...)]
        (save {:kind "test" :value "test"})
        (find-by-kind "test"))

### Creating a datastore piecemeal

## License

Copyright © 2012 8th Light, Inc.

Distributed under the Eclipse Public License, the same as Clojure.
