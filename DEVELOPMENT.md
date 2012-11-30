# Hyperion Development Guide

## Requirements

 * Java 1.6+
 * Leiningen 2
 * Ruby + Rake

## Structure

Hyperion is split into serveral submodule.  The root, and client facing, module is 'api'.  All other modules depend on this module.

## Setup

### Checkouts

Rather than install the api and sql modules, where are needed by other modules, we are using Leiningen's checkouts feature.  To setup all the checkouts, run:

    rake checkouts

