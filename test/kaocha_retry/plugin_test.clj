(ns kaocha-retry.plugin-test
  (:require [kaocha-retry.plugin :as sut]
            [kaocha.plugin :as plugin]
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

(defn run-plugin-hook [hook init & extra-args]
  (let [chain (plugin/load-all [:kaocha-retry.plugin/retry])]
    (apply plugin/run-hook* chain hook init extra-args)))

(deftest config-test
  (is (= #::sut{:retry? true :max-retries 3 :retry-interval 100}
         (run-plugin-hook :kaocha.hooks/config {})))

  (is (= false (::sut/retry? (run-plugin-hook :kaocha.hooks/config
                                              {::sut/retry? false}))))

  (is (= true (::sut/retry? (run-plugin-hook :kaocha.hooks/config
                                             {::sut/retry? false
                                              :kaocha/cli-options {:retry true}}))))

  (is (= 9 (::sut/max-retries (run-plugin-hook :kaocha.hooks/config
                                               {::sut/max-retries 9}))))

  (is (= 1 (::sut/max-retries (run-plugin-hook :kaocha.hooks/config
                                               {::sut/max-retries 9
                                                :kaocha/cli-options {:max-retries 1}}))))

  (is (= 5000 (::sut/retry-interval (run-plugin-hook :kaocha.hooks/config
                                                     {::sut/retry-interval 5000}))))

  (is (= 200 (::sut/retry-interval (run-plugin-hook :kaocha.hooks/config
                                                    {::sut/retry-interval 5000
                                                     :kaocha/cli-options {:retry-interval 200}})))))
