;; Copyright © Manetu, Inc.  All rights reserved

(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [manetu.temporal-benchmark.main :as main]))

(defn run
  [params]
  (apply main/-app (clojure.string/split params #" ")))
