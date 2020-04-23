(ns metrix-lawg.logger
  (:require [clojure.data.json :as json]
            [metrix-lawg.util :as lawg.util]
            [metrix-lawg.core :as lawg]
            [taoensso.timbre :as timbre :refer [info infof]])
  (:import [metrix_lawg.core TimeMetric
                             SuccessMetric
                             ErrorMetric
                             ExitCodeMetric]
           [com.amazonaws.services.cloudwatch AmazonCloudWatch
                                              AmazonCloudWatchClientBuilder]
           [com.amazonaws.services.cloudwatch.model Dimension
                                                    MetricDatum
                                                    PutMetricDataRequest
                                                    PutMetricDataResult
                                                    StandardUnit]
           [com.amazonaws.services.sns AmazonSNS
                                       AmazonSNSClientBuilder]
           [com.amazonaws.services.sns.model PublishRequest
                                             PublishResult])
  (:gen-class))



;; =============
;; * Protocols *
;; =============

(defprotocol PApplicationMetricLogger
  ;; Writes app specific metrics
  (runtime   [this action value])
  (error     [this action value])
  (success   [this action])
  (failure   [this action])
  (exit-code [this action value]))



;; ---
;; - Impls
;; ---

(defrecord AppMetricLogger [app writer]
  PApplicationMetricLogger
  (runtime
    [this action value]
    (let [metric (TimeMetric. app action)]
      (.write writer metric value)))

  (success
    [this action]
    (let [metric (SuccessMetric. app action)]
      (.write writer metric 1)))

  (failure
    [this action]
    (let [metric (SuccessMetric. app action)]
      (.write writer metric 0)))

  (error
    [this action value]
    (let [metric (ErrorMetric. app action value)]
      (.write writer metric 1)))

  (exit-code
    [this action value]
    (let [metric (ExitCodeMetric. app action)]
      (.write writer metric value))))


;; ---
;; - Factory
;; ---

(defn application-metric-logger
  [app-name & {:keys [writer args]
               :or {writer :stdout
                    args   {}}}]
  (AppMetricLogger. app-name (lawg/metric-writer writer :args args)))


;; ========
;; - Main -
;; ========

(defn parse-arn
  [args]
  (nth args 0))

(defn parse-namespace
  [args]
  (nth args 1))


;; Test!!
(defn -main
  [& args]
  (let [sns-arn   (parse-arn args)
        namesapce (parse-namespace args)
        foo-app   (application-metric-logger "foo-app")
        bar-app   (application-metric-logger "bar-app")
        sns-app   (application-metric-logger "test" :writer :sns :args {:topic-arn sns-arn})
	cw-app    (application-metric-logger "test" :writer :cloudwatch :args {:namespace namespace})]
    (.runtime   foo-app "insert" (* 10 (rand)))
    (.success   foo-app "insert")
    (.failure   foo-app "delete")
    (.error     foo-app "query" (java.lang.IllegalArgumentException. "Foo"))
    (.exit-code foo-app "insert" 255)
    (.success   bar-app "query")
    (.runtime   bar-app "query" (* 10 (rand)))))
