;; Copyright © Manetu, Inc.  All rights reserved

(ns manetu.temporal-benchmark.commands.combo
  (:require [manetu.temporal-benchmark.utils :refer [exec-command]]
            [manetu.temporal-benchmark.commands.client.core :as client]
            [manetu.temporal-benchmark.commands.worker :as worker]))

(def command "combo")
(def description
  "Creates an all-in-one benchmark run by combining both 'client' and 'worker' operations into one process.")

(defn exec [options client]
  (let [w (worker/start options client)]
    (client/start options client)
    (worker/stop w)))

(def spec {:description description
           :fn (partial exec-command {:command        command
                                      :description    description
                                      :options-spec   (concat client/options-spec worker/options-spec)
                                      :fn exec})})
