;; Copyright © Manetu, Inc.  All rights reserved

(ns manetu.temporal-benchmark.commands.worker
  (:require [temporal.workflow :refer [defworkflow]]
            [temporal.client.worker :as worker]
            [manetu.temporal-benchmark.utils :refer [exec-command]]))

(def command "worker")
(def description "Creates a benchmark worker to process temporal workflow/activities.  Requires external client.")

(defworkflow benchmark-workflow
  [_]
  :ok)

(def options-spec
  [[nil "--max-concurrent-workflow-task-pollers NUM" "Number of simultaneous poll requests on workflow task queue"
    :default 25
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--max-concurrent-workflow-task-execution-size NUM" "Maximum number of simultaneously executed workflow tasks"
    :default 500
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--max-workflow-thread-count NUM" "Maximum number of threads available for workflow execution"
    :default 1200
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--workflow-cache-size NUM" "Maximum number of cached workflows"
    :default 600
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]
   [nil "--[no-]using-virtual-workflow-threads" "Use Virtual Threads for workflow threads (requires JDK 21+)"
    :default false]])

(defn start [{:keys [temporal-taskqueue
                     max-concurrent-workflow-task-pollers
                     max-concurrent-workflow-task-execution-size
                     max-workflow-thread-count
                     workflow-cache-size
                     using-virtual-workflow-threads] :as options} client]
  (worker/start client
                {:task-queue temporal-taskqueue
                 :max-concurrent-workflow-task-pollers max-concurrent-workflow-task-pollers
                 :max-concurrent-workflow-task-execution-size max-concurrent-workflow-task-execution-size}
                {:max-workflow-thread-count max-workflow-thread-count
                 :workflow-cache-size workflow-cache-size
                 :using-virtual-workflow-threads using-virtual-workflow-threads}))

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