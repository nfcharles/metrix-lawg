(ns metrix-lawg.logger.impl.aaml
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

(defprotocol IApplicationActionLogger
  (runtime   [this value]
             [this value metadata])
  (success   [this]
             [this metadata])
  (failure   [this]
             [this metadata])
  (error     [this value]
             [this value metadata])
  (exit-code [this value]
             [this value metadata]))


;; ==================
;; *  Record Impls  *
;; ==================

;; --------------------------
;; - Action Specific Logger -
;; --------------------------

(defrecord ApplicationActionMetricLogger [action logger]
  IApplicationActionLogger
  (runtime
    [this value]
    (.runtime logger action value))

  (runtime
    [this value metadata]
    (.runtime logger action value metadata))

  (success
    [this]
    (.success logger action))

  (success
    [this metadata]
    (.success logger action metadata))

  (failure
    [this]
    (.failure logger action))

  (failure
    [this metadata]
    (.failure logger action metadata))

  (error
    [this value]
    (.error logger action value))

  (error
    [this value metadata]
    (.error logger action value metadata))

  (exit-code
    [this value]
    (.exit-code logger action value))

  (exit-code
    [this value metadata]
    (.exit-code logger action value metadata)))
