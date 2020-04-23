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
  (runtime   [this value] [this action value])
  (error     [this value] [this action value])
  (success   [this]       [this action])
  (failure   [this]       [this action])
  (exit-code [this value] [this action value]))



;; -----------
;; -  Impls  -
;; -----------

;; ---
;; Generic Logger (Action Agnostic)
;; ---

(deftype ApplicationMetricLogger [app writer]
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
;; - Action Specific Logger
;; ---


(deftype ApplicationActionMetricLogger [action logger]
  PApplicationMetricLogger
  (runtime
    [this value]
    (.runtime logger action value))

  (success
    [this]
    (.success logger action))

  (failure
    [this]
    (.failure logger action))

  (error
    [this value]
    (.error logger action value))

  (exit-code
    [this value]
    (.exit-code logger action value)))


;; ---
;; - Factory
;; ---

(defn application-metric-logger
  [app-name & {:keys [writer args]
               :or {writer :stdout
                    args   {}}}]
  (ApplicationMetricLogger. app-name (lawg/metric-writer writer :args args)))


(defn application-action-metric-logger
  [app-name action & {:keys [writer args]
                      :or {writer :stdout
                           args   {}}}]
  (ApplicationActionMetricLogger. action (application-metric-logger app-name :writer writer :args args)))



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
	cw-app    (application-metric-logger "test" :writer :cloudwatch :args {:namespace namespace})
        aaml      (application-action-metric-logger "biz-app" "query")]
    (.runtime   foo-app "insert" (* 10 (rand)))
    (.success   foo-app "insert")
    (.failure   foo-app "delete")
    (.error     foo-app "delete" (java.lang.IllegalArgumentException. "Foo"))
    (.exit-code foo-app "insert" 255)
    (.failure   aaml)
    (.success   aaml)
    (.runtime   aaml (* 10 (rand)))))
