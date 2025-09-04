;; Copyright © Manetu, Inc.  All rights reserved

(ns manetu.temporal-benchmark.commands.worker
  (:require [clojure.set :refer [rename-keys]]
            [promesa.exec :as p.exec]
            [taoensso.timbre :as log]
            [temporal.workflow :refer [defworkflow]]
            [temporal.activity :refer [defactivity] :as a]
            [temporal.promise :as tp]
            [temporal.client.worker :as worker]
            [manetu.temporal-benchmark.utils :refer [exec-command]]))

(def command "worker")
(def description "Creates a benchmark worker to process temporal workflow/activities.  Requires external client.")

(defactivity benchmark-activity
  [_ args]
  (log/debug "args:" args)
  :ok)

(defworkflow benchmark-workflow
  [{:keys [batch-size batch-nr]}]
  (dotimes [i batch-nr]
    @(tp/all (map #(a/invoke benchmark-activity {:batch i :task %}) (range batch-size))))
  :ok)

(def options-spec
  [[nil "--max-concurrent-activity-task-pollers NUM" "Number of simultaneous poll requests on workflow task queue"
    :default 100
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--max-concurrent-activity-execution-size NUM" "Maximum number of simultaneously executed workflow tasks"
    :default 500
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--max-concurrent-workflow-task-pollers NUM" "Number of simultaneous poll requests on workflow task queue"
    :default 100
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--max-concurrent-workflow-task-execution-size NUM" "Maximum number of simultaneously executed workflow tasks"
    :default 500
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--max-workflow-thread-count NUM" "Maximum number of threads available for workflow execution"
    :default 2200
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--workflow-cache-size NUM" "Maximum number of cached workflows"
    :default 1000
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--[no-]using-virtual-workflow-threads" "Use Virtual Threads for workflow threads (requires JDK 21+)"
    :default p.exec/vthreads-supported?]])

(defn start [options client]
  (worker/start client
                (-> (select-keys options [:temporal-taskqueue
                                          :max-concurrent-activity-task-pollers
                                          :max-concurrent-activity-execution-size
                                          :max-concurrent-workflow-task-pollers
                                          :max-concurrent-workflow-task-execution-size])
                    (rename-keys {:temporal-taskqueue :task-queue}))
                (select-keys options [:max-workflow-thread-count
                                      :workflow-cache-size
                                      :using-virtual-workflow-threads])))

(def stop worker/stop)

(defn exec [options client]
  (let [w (start options client)]
    (println "Worker running.  Press CNTL-C to exit")
    (deref (promise))
    (worker/stop w)))

(def spec {:description description
           :fn (partial exec-command {:command        command
                                      :description    description
                                      :options-spec   options-spec
                                      :fn exec})})