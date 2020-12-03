(ns kaocha.plugin.retry
  "Instrument/unstrument namespaces with Orchestra, to get validation of function
  arguments and return values based on clojure.spec.alpha."
  (:require [clojure.test :as te]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.hierarchy :as h]
            [kaocha.testable :as t]
            [kaocha.result :as r]))

(def max-retries 3)
(def wait-time 100)
;; this needs to be reset in a fixture before every test
(def current-retries (atom {}))
(def report-history (atom []))
(def to-report (atom nil))

(defplugin kaocha.plugin/retry
  (wrap-run [run test-plan]
    (fn [& args]
      (let [result
            (with-redefs [te/report (fn [e]
                                      (swap! report-history conj e)
                                      e)]
              (apply run args))]

        (doseq [{:keys [type] :as r} @report-history]
          (if (= :pass type)
            (te/report r)
            (let [test-id (-> r :kaocha/testable :kaocha.testable/id)]
              (println "test-id" test-id
                       "current-retries" @current-retries)
              (when (= max-retries (get @current-retries test-id))
                (te/report r)))))
        result)))

  (post-test [test test-plan]
    (let [t-id (:kaocha.testable/id test)
          curr (get @current-retries t-id 0)]
      (when (and
             (h/leaf? test)
             (r/failed-one? test)
             (< curr max-retries))

        (swap! current-retries assoc t-id (inc curr))
        (println "Current retries" @current-retries)
        (Thread/sleep wait-time)
        (t/run-testable test test-plan))
      test)))
