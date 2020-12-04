(ns kaocha.plugin.retry
  "Instrument/unstrument namespaces with Orchestra, to get validation of function
  arguments and return values based on clojure.spec.alpha."
  (:require [clojure.test :as te]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.hierarchy :as h]))

;; TODO: make these two configurable
(def default-max-retries 3)
(def default-wait-time 100)
(def current-retries (atom {}))
(def to-report (atom []))

(defn- with-capture-report [t]
  (with-redefs [te/report (fn [& args]
                            (reset! to-report args))]
    (t)))

(defn run-with-retry [max-retries wait-time t test-id]
  (fn []
    (loop [passed? (with-capture-report t)]
      (let [attempts (get @current-retries test-id)
            report #(apply te/report @to-report)]
        (if passed?
          (do (report) true)
          (if (= attempts max-retries)
            (do (report) false)
            (do
              (Thread/sleep wait-time)
              (swap! current-retries
                     assoc
                     test-id
                     (inc attempts))

              (recur (with-capture-report t)))))))))

(defplugin kaocha.plugin/retry
  (pre-test [testable test-plan]
   (let [max-retries (::retry-max-tries test-plan 3)
         wait-time (::retry-wait-time test-plan default-wait-time)
         test-id (:kaocha.testable/id testable)]
     (swap! current-retries assoc test-id 0)
     (cond-> testable
       (h/leaf? testable)
       (-> (update :kaocha.testable/wrap
                   conj
                   (fn [t]
                     (run-with-retry max-retries
                                     wait-time
                                     t
                                     test-id))))))))
