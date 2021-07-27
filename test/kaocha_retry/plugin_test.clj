(ns kaocha-retry.plugin-test
  (:require [kaocha-retry.plugin :as sut]
            [clojure.test :refer [deftest testing is are]]))

(deftest retry-test
  (testing "should retry"
    (are [test-plan testable retry?] (= retry? (sut/should-retry? test-plan testable))
      {:kaocha-retry.plugin/flakey-tests-regexes [], ::sut/retry? true}
      {:kaocha.var/name "ns/test-name", :kaocha.testable/type :kaocha.testable.type/leaf}
      true))

  (testing "Can report flakey tests")

  (testing "Passing tests don't get affected"))
