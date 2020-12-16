(ns kaocha-retry.plugin
  "Plugin to retry tests multiple times, useful for flakey tests."
  (:require [clojure.test :as te]
            [kaocha.hierarchy :as h]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.testable :as testable]))

(def default-max-retries 3)
(def default-wait-time 100)

(def current-retries (atom 0))

(defn- with-capture-report [t to-report]
  (with-redefs [te/report (fn [& args]
                            (swap! to-report concat args))]

    (try
      (t)
      [(empty? (filter h/fail-type? @to-report)) nil]
      (catch Exception e
        [false e]))))

(defn run-with-retry [t max-retries wait-time]
  (fn []
    (loop [attempts 0]
      (reset! current-retries attempts)
      (let [to-report (atom [])
            [passed? exc] (with-capture-report t to-report)
            report #(doseq [tr @to-report]
                      (te/report tr))]
        (if passed?
          (do (report) true)
          (if (= attempts max-retries)
            (do (report)
                (throw exc))
            (do
              (Thread/sleep wait-time)
              (recur (inc attempts)))))))))

(defn format-retries [retried max-retries]
  (let [[success fails]
        (->> retried
             (sort-by second)
             (split-with #(< (second %) max-retries)))]
    (when (seq fails)
      (println (format "* Tests failed even after %s retries" max-retries))
      (doseq [[t-id] fails]
        (println (format "- %s" t-id))))

    (when (seq success)
      (println "* Tests succeeded after retrying `n` times")
      (doseq [[t-id retries] success]
        (println (format "- %s: %s" t-id retries))))))

(defplugin kaocha-retry.plugin/retry
  (cli-options [opts]
    (conj opts
          [nil "--[no-]retry" "Retry tests"]
          [nil "--max-retries NUM" "Number of times to retry the tests"
           :parse-fn #(Integer/parseInt %)]
          [nil "--retry-interval INTERVAL" "How many milliseconds to wait before retrying"
           :parse-fn #(Integer/parseInt %)]))

  (config [config]
    (let [retry? (get-in config
                         [:kaocha/cli-options :retry]
                         true)
          max-retries (get-in config
                              [:kaocha/cli-options :max-retries]
                              default-max-retries)
          retry-interval (get-in config
                                 [:kaocha/cli-options :retry-interval]
                                 default-wait-time)]

      (assoc config
             ::retry? retry?
             ::max-retries max-retries
             ::retry-interval retry-interval)))

  ;; can I get the retry? config from the config to each testable??
  (pre-test [testable test-plan]
    (reset! current-retries 0)
    ;; these two are not actually being fetched correctly
    (if (and (::retry? test-plan) (h/leaf? testable))
      (update testable
              :kaocha.testable/wrap
              conj
              (fn [t]
                (run-with-retry t
                                (::max-retries test-plan)
                                (::retry-interval test-plan))))
      testable))

  (post-test [testable test-plan]
    (cond-> testable
      (pos? @current-retries) (assoc ::retries @current-retries)))

  (post-summary [test-result]
    (let [retried
          (for [t (testable/test-seq test-result)
                :let [retries (::retries t)]
                :when (and retries
                           (h/leaf? t)
                           (pos? retries))]
            [(:kaocha.testable/id t) retries])]
      (format-retries retried (::max-retries test-result))
      test-result)))
