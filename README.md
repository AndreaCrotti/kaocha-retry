# Retry plugin for kaocha

Kaocha plugin to re-run tests with Kaocha automatically, cleaning up
the reporting to make sure we don't report failed tests.

## How to use it

Add the dependency to your project.clj/deps.edn file with:

[![Clojars Project](https://img.shields.io/clojars/v/kaocha-retry.svg)](https://clojars.org/kaocha-retry)

And add `:kaocha-retry.plugin/retry` to your list of Kaocha plugins.

Configuration:
