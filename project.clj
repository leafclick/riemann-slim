(defproject com.leafclick/riemann-slim "0.3.9"
  :description
"A network event stream processor. Intended for analytics, metrics, and alerting; and to glue various monitoring systems together."
  :url "https://github.com/riemann/riemann"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
;  :warn-on-reflection true
  :jvm-opts ["-server" "-Xms1024m" "-Xmx1024m" "-XX:+CMSParallelRemarkEnabled" "-XX:+AggressiveOpts" "-XX:+CMSClassUnloadingEnabled" "-XX:+IgnoreUnrecognizedVMOptions"]
  :maintainer {:email "aphyr@aphyr.com"}
  :dependencies [
    [org.clojure/clojure "1.11.1"]
    [org.clojure/algo.generic "0.1.3"]
    [org.clojure/math.numeric-tower "0.0.5"]
    [org.clojure/tools.logging "1.2.4"]
    [org.clojure/tools.nrepl "0.2.13"]
    [org.clojure/core.cache "1.0.225"]
    [org.clojure/data.priority-map "1.1.0"]
    [org.clojure/java.classpath "1.0.0"]
    [org.slf4j/log4j-over-slf4j "2.0.9"]
    [javax.xml.bind/jaxb-api "2.4.0-b180830.0359"]
    [ch.qos.logback/logback-classic "1.4.11"]
    [clj-commons/pomegranate "1.2.23"
     :exclusions [org.codehaus.plexus/plexus-utils]]
    ; for pomegranate
    [org.codehaus.plexus/plexus-utils "4.0.0"]
    [clj-http/clj-http "3.12.3"]
    [com.cnuernber/charred "1.032"]
    [clj-librato/clj-librato "0.0.5"]
    [clj-time/clj-time "0.15.2"]
    [clj-wallhack/clj-wallhack "1.0.1"]
    [com.boundary/high-scale-lib "1.0.6"]
    [interval-metrics/interval-metrics "1.0.1"]
    [clj-antlr/clj-antlr "0.2.4"]
    [io.netty/netty-all "4.1.97.Final"]
    [com.google.protobuf/protobuf-java "3.24.3"]
    [riemann-clojure-client/riemann-clojure-client "0.5.4" :exclusions [com.google.protobuf/protobuf-java]]
    [less-awful-ssl/less-awful-ssl "1.0.6"]
    [slingshot/slingshot "0.12.2"]
    [org.apache.kafka/kafka-clients "3.5.1"]
    [spootnik/kinsky "0.1.26"]
    [pjstadig/humane-test-output "0.11.0"]
    [com.fasterxml.jackson.core/jackson-core "2.15.2"]
    [com.fasterxml.jackson.core/jackson-databind "2.15.2"]]
  :managed-dependencies [[org.jsoup/jsoup "1.16.1"]]
  :plugins [[lein-codox "0.10.6"]

            [lein-difftest "2.0.0"]
            [lein-ancient "0.6.15"]
            [lein-rpm "0.0.6"
             :exclusions [org.apache.maven/maven-plugin-api
                          org.codehaus.plexus/plexus-container-default
                          org.codehaus.plexus/plexus-utils
                          org.clojure/clojure
                          classworlds]]
            ; for lein-rpm
            [org.apache.maven/maven-plugin-api "2.0"]
            [org.codehaus.plexus/plexus-container-default
             "2.0.0"]
            [org.codehaus.plexus/plexus-utils "3.2.0"]
            [classworlds "1.1"]
            [test2junit "1.3.3"]]
  :test2junit-output-dir "target/test2junit"
  :profiles {:dev {:jvm-opts ["-XX:-OmitStackTraceInFastThrow"]
;                              "-Dcom.sun.management.jmxremote"
;                              "-XX:+UnlockCommercialFeatures"
;                              "-XX:+FlightRecorder"]
                   :dependencies [[criterium "0.4.6"]]}
             :uberjar {:aot :all}}
  :test-selectors {:default (fn [x] (not (or (:integration x)
                                             (:time x)
                                             (:bench x))))
                   :integration :integration
                   :email :email
                   :sns :sns
                   :graphite :graphite
                   :influxdb :influxdb
                   :influxdb2 :influxdb2
                   :kairosdb :kairosdb
                   :librato :librato
                   :hipchat :hipchat
                   :nagios :nagios
                   :opentsdb :opentsdb
                   :rabbitmq :rabbitmq
                   :time :time
                   :bench :bench
                   :focus :focus
                   :slack :slack
                   :druid :druid
                   :cloudwatch :cloudwatch
                   :datadog :datadog
                   :stackdriver :stackdriver
                   :xymon :xymon
                   :shinken :shinken
                   :telegram :telegram
                   :blueflood :blueflood
                   :opsgenie :opsgenie
                   :boundary :boundary
                   :prometheus :prometheus
                   :elasticsearch :elasticsearch
                   :netuitive :netuitive
                   :kafka :kafka
                   :pushover :pushover
                   :msteams :msteams
                   :clickhouse :clickhouse
                   :all (fn [_] true)}
;;  :javac-options     ["-target" "1.6" "-source" "1.6"]
  :java-source-paths ["src/"]
  :java-source-path "src/"
;  :aot [riemann.bin]
  :main riemann.bin
  :codox {:output-path "site/api"
          :source-uri "https://github.com/riemann/riemann/blob/{version}/{filepath}#L{line}"
          :metadata {:doc/format :markdown}}
)
