;; Copyright © Manetu, Inc.  All rights reserved

(ns manetu.temporal-benchmark.commands.worker
  (:require [manetu.temporal-benchmark.utils :refer [exec-command]]
            [temporal.workflow :refer [defworkflow]]
            [temporal.client.worker :as worker]))

(def command "worker")
(def description "Creates a benchmark worker to process temporal workflow/activities.  Requires external client.")

(defworkflow benchmark-workflow
  [_]
  :ok)

(def options-spec
  [])

(defn start [{:keys [temporal-taskqueue] :as options} client]
  (worker/start client {:task-queue temporal-taskqueue}))

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