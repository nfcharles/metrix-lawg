# metrix-lawg
[![Build Status](https://travis-ci.org/nfcharles/metrix-lawg.svg?branch=master)](https://travis-ci.org/nfcharles/metrix-lawg)
[![codecov](https://codecov.io/gh/nfcharles/metrix-lawg/branch/master/graph/badge.svg)](https://codecov.io/gh/nfcharles/metrix-lawg)
[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.nfcharles/metrix-lawg.svg)](https://clojars.org/org.clojars.nfcharles/metrix-lawg)


Metrics logging utility

## Intro

`metrix-lawg` provides logging features with various backend implementations.

Sample Output: `stdout`
```clojure
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - foo-app.insert.runtime 8.10629431440315
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - foo-app.insert.success 1
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - foo-app.delete.success 0
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - foo-app.query.error.illegal-argument-exception 1
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - foo-app.insert.exit-code 255
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - bar-app.query.success 1
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - bar-app.query.runtime 8.246678392849057
```

## API

### Default Application Logger (Action Agnostic)

```clojure
(ns example
  (:require [metrix-lawg.logger :as lawg.log]))

(def app (lawg.log/application-metric-logger "test-app"))


;; Application `action`; represents a quantifiable behavior of the app that you want to log
(def action "insert")


;; Log success
(.success app action)

;; Log failure
(.failure app action)

;; Log runtime
(.runtime app action 10.12345)

;; Log exit code
(.exit-code app action 1)

;; Log error
(.error app action (java.lang.Exception. "foo"))
```

### Action Specific Logger

```clojure

(def logger (lawg.log/application-action-metric-logger "biz-app" "query"))

(.success logger)
(.runtime logger (timer))
```

```bash
> run-example
20-04-23 21:52:28 ncharles-XPS-13-9360 INFO [metrix-lawg.core:87] - biz-app.query.success 1
20-04-23 21:52:28 ncharles-XPS-13-9360 INFO [metrix-lawg.core:87] - biz-app.query.runtime 9.25572592520998
```

#### Custom Loggers

You can make your own application metric loggers using the base `Metric` and `MetricWriter` protocols.

## Implementations


### Standard Out (Default)

```clojure
;; Get application logger

(ns example
  (:require [metrix-lawg.logger :as lawg.log]))

;; Get stdout logger
(let [app (lawg.log/application-metric-logger "test-app")]
  (.success app "insert"))
```

```bash
> run-example
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - test-app.insert.success 1
```

### Cloudwatch

```clojure
;; Get application logger

(ns example
  (:require [metrix-lawg.logger :as lawg.log]))

(def project-namesapce "/env/test-project")
(let [app (lawg.log/application-metric-logger "test-app" :writer :cloudwatch :args {:namespace project-namespace})]
  (try
    ...
    (catch
      (.failure app "exec"))))
```


### SNS

```clojure
;; Get application logger

(ns example
  (:require [metrix-lawg.logger :as lawg.log]))

(def topic-arn "arn://...")
(let [app (lawg.log/application-metric-logger "test-app" :writer :sns :args {:topic-arn topic-arn})]
  (try
    (let []
      ...
      (.success app "exec"))
    (catch
      (.failure app "exec"))))

```

```bash
> run-example
20-04-23 15:52:44 ncharles-XPS-13-9360 INFO [metrix-lawg.core:133] - SNS_PUBLISH_RESPONSE=a63019dc-77c2-54dc-a90d-4ec7cbce8cca
```

#### With Metadata

Pass in arbitrary json object as metadata

```clojure
;; Get application logger

(ns example
  (:require [metrix-lawg.logger :as lawg.log]))

(def topic-arn "arn://...")
(let [app (lawg.log/application-metric-logger "test-app" :writer :sns :args {:topic-arn topic-arn})]
  (try
    (let []
      ...
      (.success app "exec" {:runtime-config {:foo 1 :bar 2}}))
    (catch
      (.failure app "exec" {:runtime-config {:foo 1 :bar 2}}))))
```

## License

Copyright © 2020 Navil Charles

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
