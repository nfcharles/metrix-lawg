(defproject org.clojars.nfcharles/metrix-lawg "0.2.0"
  :description "Metrics logger supporting different impl - stdout, cloudwatch, sns"
  :url "http://github.com/nfcharles/metrix-lawg"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.amazonaws/aws-java-sdk-cloudwatch "1.11.470"]
                 [com.amazonaws/aws-java-sdk-sns "1.11.470"]
                 [com.taoensso/timbre "4.10.0"]]

  :repositories [["releases" {:url "https://repo.clojars.org" :creds :gpg}]]
)
