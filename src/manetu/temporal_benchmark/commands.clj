;; Copyright © Manetu, Inc.  All rights reserved

(ns manetu.temporal-benchmark.commands
  (:require [clojure.pprint :refer [cl-format]]
            [manetu.temporal-benchmark.commands.client.core :as client]
            [manetu.temporal-benchmark.commands.worker :as worker]
            [manetu.temporal-benchmark.commands.combo :as combo]))

(def command-map
  {client/command client/spec
   worker/command worker/spec
   combo/command combo/spec})

(defn get-handler
  [subcommand]
  (some-> (get command-map subcommand) :fn))

(defn render-description []
  (mapv (fn [[command {:keys [description]}]]
          (cl-format nil (str " - " command ": ~24T" description)))
        command-map))
