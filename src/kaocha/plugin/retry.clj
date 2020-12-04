(ns kaocha.plugin.retry
  "Instrument/unstrument namespaces with Orchestra, to get validation of function
  arguments and return values based on clojure.spec.alpha."
  (:require [clojure.test :as te]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.hierarchy :as h]))

(def max-retries 3)
(def wait-time 100)
(def current-retries (atom {}))

(defplugin kaocha.plugin/retry
  (pre-test [testable test-plan]
    (let [test-id (-> testable :kaocha.testable/id)]
      (swap! current-retries assoc test-id 0)
      (cond-> testable
        (h/leaf? testable)
        (-> (update :kaocha.testable/wrap
                    conj
                    (fn [t]
                      (fn []
                        (loop [passed? (t)]
                          (let [attempts (get @current-retries test-id)]
                            (or passed?
                                (if (= attempts max-retries)
                                  false
                                  (do
                                    (Thread/sleep wait-time)
                                    (swap! current-retries
                                           assoc
                                           test-id
                                           (inc attempts))

                                    (recur (t)))))))))))))))
