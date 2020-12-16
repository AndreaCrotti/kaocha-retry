# Retry plugin for kaocha

Flakey tests are an issue that affect many people, and they should be
handled properly to avoid the flakeyness.
However in some cases you might still want to retry flakey tests, in case
the failures depend on external transient factors that are hard to control.

This [Kaocha](https://github.com/lambdaisland/kaocha) plugin will
re run tests automatically in a transparent way, but making sure that the
retried tests get reported at the end.

## How to use it

Add the dependency to your project.clj/deps.edn file with:

[![Clojars Project](https://img.shields.io/clojars/v/kaocha-retry.svg)](https://clojars.org/kaocha-retry)

You can enable this plugin globally for all test suites adding `:kaocha-retry.plugin/retry` to your list of Kaocha plugins.

If you enable it globally you can still disable it by passing:

    --no-retry

Otherwise you can just enable it for your own test run with enabling it with:

    --with-plugin :kaocha-retry.plugin/retry

You can also configure the number of retries and the wait interval between runs with:

    --retries $NUMBER_OF_RETRIES

and:

    --retry-interval $RETRY_INTERVAL_IN_MILLISECONDS
