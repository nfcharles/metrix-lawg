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




;; ================
;; *  Interfaces  *
;; ================


(defprotocol IMetric
  (name [this]))

(defprotocol IMetricWriter
  (write [this metric value]))


;; ==================
;; *  Record Impls  *
;; ==================


;; =============
;; -  Metrics  -
;; -------------

(defrecord TimeMetric [app action]
  metrix_lawg.core.IMetric
  (name
    [this]
    (format "%s.%s.runtime" app action)))

(defrecord SuccessMetric [app action]
  metrix_lawg.core.IMetric
  (name
    [this]
    (format "%s.%s.success" app action)))

(defrecord ErrorMetric [app action err]
  metrix_lawg.core.IMetric
  (name
    [this]
    (format "%s.%s.error.%s" app action (lawg.util/format-error err))))

(defrecord ExitCodeMetric [app action]
  metrix_lawg.core.IMetric
  (name
    [this]
    (format "%s.%s.exit-code" app action)))



;; ===================
;; -  MetricWriters  -
;; -------------------

;; -----------------
;; - Stdout Writer -
;; -----------------

(defrecord StdoutMetricWriter []
  metrix_lawg.core.IMetricWriter
  (write
    [this metric value]
    (try
      (infof "%s %s" (.name metric) value)
      (catch Exception e
        (errorf "Error writing to stdout: %s" e)))))


;; ---------------------
;; - CloudWatch Writer -
;; ---------------------

(defn metric-datum ^MetricDatum
  [^metrix_lawg.core.IMetric metric value]
  (-> (MetricDatum.)
      (.withMetricName (.name metric))
      (.withUnit (StandardUnit/None))
      (.withValue (double value))))

(defrecord CloudWatchMetricWriter [^AmazonCloudWatch cw args]
  metrix_lawg.core.IMetricWriter
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


;; --------------
;; - SNS Writer -
;; --------------

(defn -wrtj
  [data]
  (json/write-str data))

(defn sns-message
  [^metrix_lawg.core.IMetric metric value]
  (if (:metadata metric)
    (-wrtj {:metric {:name (.name metric) :value value :meta (:metadata metric)}})
    (-wrtj {:metric {:name (.name metric) :value value}})))

(defrecord SNSMetricWriter [^AmazonSNS sns args]
  metrix_lawg.core.IMetricWriter
  (write [this metric value]
    (try
      (let [^PublishRequest publishRequest (PublishRequest. (:topic-arn args) (sns-message metric value))
            ^PublishResult publishResponse (.publish sns publishRequest)]
        (infof "SNS_PUBLISH_RESPONSE=%s" (.getMessageId publishResponse)))
      (catch Exception e
        (errorf "Error writing to sns: %s" e)))))


;; =========================
;; -  MetricWriter Factory -
;; -------------------------

(defn ^metrix_lawg.core.StdoutMetricWriter stdout-writer
  []
  (StdoutMetricWriter.))

(defn ^metrix_lawg.core.CloudWatchMetricWriter cloudwatch-writer
  [args]
  (CloudWatchMetricWriter. (AmazonCloudWatchClientBuilder/defaultClient) args))

(defn ^metrix_lawg.core.SNSMetricWriter sns-writer
  [args]
  (SNSMetricWriter. (AmazonSNSClientBuilder/defaultClient) args))

(defn ^metrix_lawg.core.IMetricWriter metric-writer
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
  IMetric
  (name [this]
    (format "%s.%s.test.count" app action)))


;; Test!!
(defn -main
  [& args]
  (let [^metrix_lawg.core.IMetricWriter std-writer  (metric-writer :stdout)
        ^metrix_lawg.core.IMetricWriter cw-writer   (metric-writer :cloudwatch)]
    (.write std-writer (TestMetric. "foo" "insert") 1)
    (.write std-writer (TestMetric. "bar" "delete") 2)
    (.write std-writer (TestMetric. "baz" "exec") 3)))
