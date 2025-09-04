;; Copyright © Manetu, Inc.  All rights reserved

(ns manetu.temporal-benchmark.commands.client.core
  (:require [clojure.core.async :as async]
            [promesa.core :as p]
            [taoensso.timbre :as log]
            [temporal.client.core :as c]
            [manetu.temporal-benchmark.time :as t]
            [manetu.temporal-benchmark.commands.worker :as worker]
            [manetu.temporal-benchmark.commands.client.pipeline :as pipeline]
            [manetu.temporal-benchmark.utils :refer [round2 exec-command]]))

(def command "client")
(def description "Creates a benchmark client to drive traffic to the specified cluster.  Requires external worker.")

(def options-spec
  [[nil "--[no-]progress" "Enable/disable progress output"
    :default true]
   [nil "--fatal-errors" "Any sub-operation failure is considered to be an application level failure"
    :default false]
   [nil "--verbose-errors" "Any sub-operation failure is logged as ERROR instead of TRACE"
    :default false]
   [nil "--batch-size NUM" "The number of parallel activities in one batch"
    :default 0
    :parse-fn parse-long
    :validate [nat-int? "Must be a non-negative integer"]]
   [nil "--batch-nr NUM" "The number of batches to run serially"
    :default 0
    :parse-fn parse-long
    :validate [nat-int? "Must be a non-negative integer"]]
   [nil "--client-concurrency NUM" "The number of parallel requests to issue"
    :default 16
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--client-requests NUM" "The total number of requests to issue"
    :default 100
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]])

(defn- execute-request [{:keys [temporal-taskqueue] :as options} client record]
  (let [w (c/create-workflow client worker/benchmark-workflow {:task-queue temporal-taskqueue})]
    (c/start w (select-keys options [:batch-size :batch-nr]))
    @(c/get-result w)))

(defn start [{:keys [client-requests client-concurrency] :as options} client]
  (let [input-ch (async/to-chan! (range 0 client-requests))
        output-ch (async/chan (* 4 client-concurrency))]
    (log/debug "processing" client-requests "requests with options:" options)
    @(-> (let [mux (async/mult output-ch)]
           (p/all [(t/now)
                   (pipeline/execute-commands options (partial execute-request options client) output-ch input-ch)
                   (pipeline/show-progress options client-requests mux)
                   (pipeline/compute-stats options client-requests mux)]))
         (p/then
          (fn [[start _ _ {:keys [successes] :as stats}]]
            (let [end (t/now)
                  d (t/duration end start)]
              (assoc stats :total-duration (round2 3 d) :rate (round2 2 (* (/ successes d) 1000))))))
         (p/then (partial pipeline/render options))
         (p/catch
          (fn [e]
            (log/error "Exception detected:" (ex-message e))
            -1)))))

(defn exec [options client]
  (start options client))

(def spec {:description description
           :fn (partial exec-command {:command        command
                                      :description    description
                                      :options-spec   options-spec
                                      :fn exec})})
