; Copyright © Manetu, Inc.  All rights reserved

(ns manetu.temporal-benchmark.metrics
  (:require [iapetos.core :as prometheus]
            [iapetos.standalone :as standalone])
  (:import [com.uber.m3.tally.experimental.prometheus PrometheusReporter PrometheusReporter$Builder TimerType]
           [io.prometheus.client CollectorRegistry]
           [com.uber.m3.tally RootScopeBuilder]
           [com.uber.m3.util Duration]))

(defn create-reporter
  ^PrometheusReporter []
  (-> (PrometheusReporter$Builder.)
      (.timerType TimerType/HISTOGRAM)
      (.registry CollectorRegistry/defaultRegistry)
      (.build)))

(def root-scope
  (delay (-> (RootScopeBuilder.)
             (.reporter (create-reporter))
             (.reportEvery (Duration/ofSeconds 1)))))

(defn start [port]
  (standalone/metrics-server prometheus/default-registry {:port port}))

(defn stop [ctx]
  (.close ctx))
