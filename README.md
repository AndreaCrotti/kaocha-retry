# Retry plugin for kaocha

Kaocha plugin to re-run tests with Kaocha automatically, cleaning up
the reporting to make sure we don't report failed tests.

## How to use it

Add the dependency to your project.clj/deps.edn file with:

[![Clojars Project](https://img.shields.io/clojars/v/kaocha-retry.svg)](https://clojars.org/kaocha-retry)

You can enable this plugin globally for all test suites adding `:kaocha-retry.plugin/retry` to your list of Kaocha plugins.

Otherwise you can just enable it for your own test run with enabling it with:

    --with-plugin :kaocha-retry.plugin/retry

You can also configure the number of retries and the wait interval between runs with:

    --retries $NUMBER_OF_RETRIES

and:

    --retry-interval $RETRY_INTERVAL_IN_MILLISECONDS
