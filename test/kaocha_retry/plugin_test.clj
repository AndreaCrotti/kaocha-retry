(ns kaocha-retry.plugin-test
  (:require [kaocha-retry.plugin :as sut]
            [clojure.test :refer [deftest testing is are]]))

(deftest retry-test
  (testing "retrying works with a leaf testable"
    (let [testable {:kaocha.var/name "ns/test-name", :kaocha.testable/type :kaocha.testable.type/leaf}]
      (are [test-plan retry?] (= retry? (sut/should-retry? test-plan testable))
        ;;with `nil` value everything should be included
        {:kaocha-retry.plugin/retrying-tests-regexes nil, ::sut/retry? true}
        true

        ;; use the full string to specify
        {:kaocha-retry.plugin/retrying-tests-regexes ["ns/test-name"], ::sut/retry? true}
        true

        {:kaocha-retry.plugin/retrying-tests-regexes ["ns/flakey-*"], ::sut/retry? true}
        false))))
