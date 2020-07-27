(ns metrix-lawg.logger.impl.aml
  (:import [metrix_lawg.core TimeMetric
                             SuccessMetric
                             ErrorMetric
                             ExitCodeMetric])
  (:gen-class))


(defn add-meta
  [metric metadata]
  (if metadata (assoc metric :metadata metadata) metric))


;; ================
;; *  Interfaces  *
;; ================

(defprotocol IApplicationLogger
  (runtime   [this action value]
             [this action value metadata])
  (success   [this action]
             [this action metadata])
  (failure   [this action]
             [this action metadata])
  (error     [this action value]
             [this action value metadata])
  (exit-code [this action value]
             [this action value metadata]))


;; ==================
;; *  Record Impls  *
;; ==================



;; ------------------------------------
;; - Generic Logger (Action Agnostic) -
;; ------------------------------------

(defrecord ApplicationMetricLogger [app writer]
  IApplicationLogger
  (runtime
    [this action value]
    (let [metric (TimeMetric. app action)]
      (.write writer metric value)))

  (runtime
    [this action value metadata]
    (let [metric (TimeMetric. app action)]
      (.write writer (add-meta metric metadata) value)))

  (success
    [this action]
    (let [metric (SuccessMetric. app action)]
      (.write writer metric 1)))

  (success
    [this action metadata]
    (let [metric (SuccessMetric. app action)]
      (.write writer (add-meta metric metadata) 1)))

  (failure
    [this action]
    (let [metric (SuccessMetric. app action)]
      (.write writer metric 0)))

  (failure
    [this action metadata]
    (let [metric (SuccessMetric. app action)]
      (.write writer (add-meta metric metadata) 0)))

  (error
    [this action value]
    (let [metric (ErrorMetric. app action value)]
      (.write writer metric 1)))

  (error
    [this action value metadata]
    (let [metric (ErrorMetric. app action value)]
      (.write writer (add-meta metric metadata) 1)))

  (exit-code
    [this action value]
    (let [metric (ExitCodeMetric. app action)]
      (.write writer metric value)))

  (exit-code
    [this action value metadata]
    (let [metric (ExitCodeMetric. app action)]
      (.write writer (add-meta metric metadata) value))))
