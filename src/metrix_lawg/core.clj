(ns metrix-lawg.core
  (:require [clojure.data.json :as json]
            [metrix-lawg.util :as lawg.util]
            [taoensso.timbre :as timbre :refer [info infof errorf]])
  (:import [com.amazonaws.services.cloudwatch AmazonCloudWatch
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



;; TODO: Break up into packages

(def default-namespace "/apps/metrix-lawg")


;; =============
;; * Protocols *
;; =============

(defprotocol PMetric
  ;;Returns metric name
  (name   [this]))

(defprotocol PMetricWriter
  ;; Writes metric to stream
  (write [this metric value]))



;; ==================
;; * Protocol Impls *
;; ==================

;; ===
;; - Metrics
;; ===


(defrecord TimeMetric [app action]
  PMetric
  (name
    [this]
    (format "%s.%s.runtime.count" app action)))

(defrecord SuccessMetric [app action]
  PMetric
  (name
    [this]
    (format "%s.%s.success.count" app action)))

(defrecord ErrorMetric [app action err]
  PMetric
  (name
    [this]
    (format "%s.%s.error.count.%s" app action (lawg.util/format-error err))))

(defrecord ExitCodeMetric [app action]
  PMetric
  (name
    [this]
    (format "%s.%s.exit-code" app action)))



;; ===
;; - MetricWriters
;; ===

;; ---
;; - Stdout Writer
;; ---

(defrecord StdoutMetricWriter []
  PMetricWriter
  (write
    [this metric value]
    (try
      (infof "%s %s" (.name metric) value)
      (catch Exception e
        (errorf "Error writing to stdout: %s" e)))))


;; ---
;; - CloudWatch Writers
;; ---

(defn metric-datum ^MetricDatum
  [metric value]
  (-> (MetricDatum.)
      (.withMetricName (.name metric))
      (.withUnit (StandardUnit/None))
      (.withValue (double value))))

(defrecord CloudWatchMetricWriter [^AmazonCloudWatch cw args]
  PMetricWriter
  (write
    [this metric value]
    (try
      (let [^PutMetricDataRequest req (-> (PutMetricDataRequest.)
                                          (.withNamespace (:namespace args))
                                          (.withMetricData (into-array MetricDatum [(metric-datum metric value)])))
            ^PutMetricDataResult res (.putMetricData cw req)]
        (infof "METRIC_DATA_RESPONSE=%s" res))
      (catch Exception e
        (errorf "Error writing to cloudwatch: %s" e)))))



;; ---
;; - SNS Writers
;; ---

(defn pack-json
  [metric value]
  (json/write-str {:name (.name metric) :value value}))

(defrecord SNSMetricWriter [^AmazonSNS sns args]
 PMetricWriter
 (write
   [this metric value]
   (try
     (let [^PublishRequest publishRequest (PublishRequest. (:topic-arn args) (pack-json metric value))
           ^PublishResult publishResponse (.publish sns publishRequest)]
       (infof "SNS_PUBLISH_RESPONSE=%s" (.getMessageId publishResponse)))
     (catch Exception e
       (errorf "Error writing to sns: %s" e)))))



;; ===
;; ** MetricWriter Factory **
;; ===

(defn stdout-writer
  []
  (StdoutMetricWriter.))

(defn cloudwatch-writer
  [args]
  (CloudWatchMetricWriter. (AmazonCloudWatchClientBuilder/defaultClient) args))

(defn sns-writer
  [args]
  (SNSMetricWriter. (AmazonSNSClientBuilder/defaultClient) args))


;; ---
;; - Factory
;; ---

(defn metric-writer
  [writer & {:keys [args]
             :or {args {}}}]
  (condp = writer
    :stdout     (stdout-writer)
    :cloudwatch (cloudwatch-writer args)
    :sns        (sns-writer args)
    (throw (java.lang.Exception. (format "Unsupported metrics writer: %s" writer)))))



;; ========
;; - Main -
;; ========

(defrecord TestMetric [app action]
  PMetric
  (name [this]
    (format "%s.%s.test.count" app action)))


;; Test!!
(defn -main
  [& args]
  (let [std-writer  (metric-writer :stdout)
        cw-writer   (metric-writer :cloudwatch)]
    (.write std-writer (TestMetric. "foo" "insert") 1)
    (.write std-writer (TestMetric. "bar" "delete") 2)
    (.write std-writer (TestMetric. "baz" "exec") 3)))
