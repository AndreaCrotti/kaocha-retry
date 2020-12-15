(ns kaocha-retry.plugin
  "Instrument/unstrument namespaces with Orchestra, to get validation of function
  arguments and return values based on clojure.spec.alpha."
  (:require [clojure.test :as te]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.testable :as testable]
            [kaocha.hierarchy :as h]))

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

(defn run-with-retry [t]
  (fn []
    (loop [attempts 0]
      (reset! current-retries attempts)
      (let [to-report (atom [])
            [passed? exc] (with-capture-report t to-report)
            report #(doseq [tr @to-report]
                      (te/report tr))]
        (if passed?
          (do (report) true)
          (if (= attempts default-max-retries)
            (do (report)
                (throw exc))
            (do
              (Thread/sleep default-wait-time)
              (recur (inc attempts)))))))))

(defn format-retries [retried]
  (let [[success fails]
        (->> retried
             (sort-by second)
             (split-with #(< (second %) default-max-retries)))]
    (when (seq fails)
      (println (format "* Tests failed even after %s retries" default-max-retries))
      (doseq [[t-id] fails]
        (println (format "- %s" t-id))))

    (when (seq success)
      (println "* Tests succeeded after retrying `n` times")
      (doseq [[t-id retries] success]
        (println (format "- %s: %s" t-id retries))))))

(defplugin kaocha-retry.plugin/retry
  (cli-config [opts]
    (conj opts [nil "--[no-]retry" "Retry tests"]))

  (config [config]
    (let [cli-flag (get-in config [:kaocha/cli-options :retry])]
      (assoc config ::retry?
             (if (some? cli-flag)
               cli-flag
               ;; should it be off by default instead??
               (::retry? config true)))))

  ;; can I get the retry? config from the config to each testable??
  (pre-test [testable test-plan]
    (reset! current-retries 0)
    ;; these two are not actually being fetched correctly
    (if (and (::retry? test-plan) (h/leaf? testable))
      (update testable
              :kaocha.testable/wrap
              conj
              (fn [t]
                (run-with-retry t)))
      testable))

  (post-test [testable test-plan]
    (cond-> testable
      (pos? @current-retries) (assoc ::retries @current-retries)))

  (post-summary [test-result]
    (let [retried
          (for [t (testable/test-seq test-result)
                :let [retries (::retries t)]
                :when (and retries (pos? retries))]
            [(:kaocha.testable/id t) retries])]
      (format-retries retried)
      test-result)))
