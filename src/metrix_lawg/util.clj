(ns metrix-lawg.util
  (:require [clojure.string :as string])
  (:gen-class))



;; ===
;; - General
;; ===

(defn format-name
  [name ]
  (-> name
      (string/replace #"([a-z])([A-Z])" "$1-$2")
      (string/replace #"([A-Z0-9]+)([A-Z])" "$1-$2")
      (string/lower-case)))

(defn format-error
  "Returns simple exception name"
  [exc]
  (format-name (.getSimpleName (.getClass exc))))
