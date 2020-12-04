(ns kaocha.plugin.retry
  "Instrument/unstrument namespaces with Orchestra, to get validation of function
  arguments and return values based on clojure.spec.alpha."
  (:require [clojure.test :as te]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.hierarchy :as h]))

(def max-retries 3)
(def wait-time 100)
(def current-retries (atom {}))
(def to-report (atom []))

(defn- with-capture-report [t]
  (with-redefs [te/report (fn [& args]
                            (reset! to-report args))]
    (t)))

(defn run-with-retry [t test-id]
  (fn []
    (loop [passed? (with-capture-report t)]
      (let [attempts (get @current-retries test-id)]
        (if passed?
          (do (apply te/report @to-report)
              true)
          (if (= attempts max-retries)
            (do
              (apply te/report @to-report)
              false)
            (do
              (Thread/sleep wait-time)
              (swap! current-retries
                     assoc
                     test-id
                     (inc attempts))

              (recur (with-capture-report t)))))))))

(defplugin kaocha.plugin/retry
  (pre-test [testable test-plan]
    (let [test-id (-> testable :kaocha.testable/id)]
      (swap! current-retries assoc test-id 0)
      (cond-> testable
        (h/leaf? testable)
        (-> (update :kaocha.testable/wrap
                    conj
                    (fn [t]
                      (run-with-retry t test-id))))))))
