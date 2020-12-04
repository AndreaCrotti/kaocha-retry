(ns simple-test
  (:require [clojure.test :refer [testing deftest is]]))

(deftest working-test
  (testing "I pass"
    (is (= 1 1))))

#_(deftest not-working-test
  (testing "I do not pass"
    (if (> (Math/random) 0.3)
      (is (= 2 1))
      (is (= 1 1)))))

(deftest not-working-test-2
  (testing "I do not pass"
    (if (> (Math/random) 0.6)
      (is (= 2 1))
      (is (= 1 1)))))
