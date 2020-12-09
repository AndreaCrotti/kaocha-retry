(ns kaocha-retry.plugin
  "Instrument/unstrument namespaces with Orchestra, to get validation of function
  arguments and return values based on clojure.spec.alpha."
  (:require [clojure.test :as te]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.testable :as testable]
            [kaocha.hierarchy :as h]))

(def default-max-retries 3)
(def default-wait-time 100)
(def to-report (atom nil))

(def current-retries (atom 0))

(defn- with-capture-report [t]
  (with-redefs [te/report (fn [& args]
                            (reset! to-report args))]
    (t)))

(defn run-with-retry [max-retries wait-time t]
  (fn []
    (loop [passed? (with-capture-report t)
           attempts 0]
      (reset! current-retries attempts)
      (let [report #(do
                      (apply te/report @to-report)
                      %)]
        (if passed?
          (report passed?)
          (if (= attempts max-retries)
            (report passed?)
            ;;TODO: should we add some exponential back off here
            ;;potentially?
            (do
              (Thread/sleep wait-time)
              (recur (with-capture-report t) (inc attempts)))))))))

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

  (pre-test [testable test-plan]
    (reset! current-retries 0)
    ;; these two are not actually being fetched correctly
    (let [max-retries (::retry-max-tries test-plan 3)
          wait-time (::retry-wait-time test-plan default-wait-time)]

      (if (and (::retry? test-plan) (h/leaf? testable))
        (-> (update testable
                    :kaocha.testable/wrap
                    conj
                    (fn [t]
                      (run-with-retry max-retries wait-time t))))
        testable)))

  (post-test [testable test-plan]
    (if (h/leaf? testable)
      ;; is this actually accessible somehow later on??
      (assoc testable ::retries @current-retries)
      testable))

  (post-summary [test-result]
    (let [retried
          (for [t (testable/test-seq test-result)
                :let [retries (::retries t)]
                :when (and retries (pos? retries))]
            [(:kaocha.testable/id t) retries])]
      (when (seq retried)
        (println "Some tests had to be retried `n` times:")
        (doseq [[t-id retries] retried]
          (println (format "%s: %s" t-id retries))))
      test-result)))
