(ns metrix-lawg.logger.core
  (:require [clojure.data.json :as json]
            [metrix-lawg.core :as lawg]
            [metrix-lawg.logger.impl.aml  :refer [->ApplicationMetricLogger]]
            [metrix-lawg.logger.impl.aaml :refer [->ApplicationActionMetricLogger]]
            [taoensso.timbre :as timbre :refer [info infof]])
  (:import [metrix_lawg.core TimeMetric
                             SuccessMetric
                             ErrorMetric
                             ExitCodeMetric])
  (:gen-class))



;; ===========
;; - Factory -
;; -----------

(defn application-metric-logger
  [app-name & {:keys [writer args]
               :or {writer :stdout
                    args   {}}}]
  (->ApplicationMetricLogger app-name (lawg/metric-writer writer :args args)))


(defn application-action-metric-logger
  [app-name action & {:keys [writer args]
                      :or {writer :stdout
                           args   {}}}]
  (->ApplicationActionMetricLogger action (application-metric-logger app-name :writer writer :args args)))



;; ========
;; - Main -
;; ========

(defn parse-arn
  [args]
  (nth args 0))

(defn parse-namespace
  [args]
  (nth args 1))



;; ====================
;; - Main Test Driver -
;; ====================

(defn -main
  [& args]
  (let [sns-arn   (parse-arn args)
        namesapce (parse-namespace args)
        foo-app   (application-metric-logger "foo-app")
        bar-app   (application-metric-logger "bar-app")
        sns-app   (application-action-metric-logger "test" "exec" :writer :sns :args {:topic-arn sns-arn})
        cw-app    (application-action-metric-logger "test" "exec" :writer :cloudwatch :args {:namespace namespace})
        runtime   (* 10 (rand))]
    (.runtime   foo-app "insert" runtime)
    (.success   foo-app "insert")
    (.failure   foo-app "delete")
    (.error     foo-app "delete" (java.lang.IllegalArgumentException. "Foo"))
    (.exit-code foo-app "insert" 255)
    (.success   sns-app {:start-date "2020-01-01" :end-date "2020-01-02" :runtime runtime})))
