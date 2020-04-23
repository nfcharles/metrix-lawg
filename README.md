# metrix-lawg


Metrics logging utility

## Intro

`metrix-lawg` provides logging features with various backend implementations.

Sample Output: `stdout`
```clojure
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - foo-app.insert.runtime.count 8.10629431440315
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - foo-app.insert.success.count 1
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - foo-app.delete.success.count 0
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - foo-app.query.error.count.illegal-argument-exception 1
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - foo-app.insert.exit-code 255
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - bar-app.query.success.count 1
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - bar-app.query.runtime.count 8.246678392849057
```

## API

### Application Logger

```clojure
(ns example
  (:require [metrix-lawg.logger:as lawg.log]))

(def app (lawg.log/application-metric-logger "test-app"))


;; Application `action`; represents a quantifiable behavior of the app that you want to log
(def action "insert")


;; Log success count
(.success app action)

;; Log failure count
(.failure app action)

;; Log runtime
(.runtime app action 10.12345)

;; Log exit code
(.exit-code app action 1)

;; Log error count
(.error app action (java.lang.Exception. "foo"))
```


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
20-04-23 14:36:23 ncharles-XPS-13-9360 INFO [metrix-lawg.core:86] - test-app.insert.success.count 1
```

### Cloudwatch

```clojure
;; Get application logger

(ns example
  (:require [metrix-lawg.logger :as lawg.log]))

(def project-namesapce "/env/test-project")
(def app (lawg.log/application-metric-logger "test-app" :cloudwatch project-namespace))
```

### SNS

```clojure
;; Get application logger

(ns example
  (:require [metrix-lawg.logger :as lawg.log]))

(def topic-arn "arn://...")
(def app (lawg.log/application-metric-logger "test-app" :topic-arn topic-arn))
```

```bash
> run-example
20-04-23 15:52:44 ncharles-XPS-13-9360 INFO [metrix-lawg.core:133] - SNS_PUBLISH_RESPONSE=a63019dc-77c2-54dc-a90d-4ec7cbce8cca
```

## License

Copyright © 2020 Navil Charles

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
