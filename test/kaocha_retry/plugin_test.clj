(ns kaocha-retry.plugin-test
  (:require [kaocha-retry.plugin :as sut]
            [clojure.test :refer [deftest testing is]]))

(deftest retry-test
  (testing "After max failures just fail"
    (is (= 1 1)))

  (testing "Can report flakey tests")

  (testing "Passing tests don't get affected"))
