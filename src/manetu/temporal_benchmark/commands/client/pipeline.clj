;; Copyright © Manetu, Inc.  All rights reserved

(ns manetu.temporal-benchmark.commands.client.pipeline
  (:require [clojure.core.async :refer [>!! <! <!! go go-loop] :as async]
            [medley.core :as m]
            [promesa.core :as p]
            [taoensso.timbre :as log]
            [progrock.core :as pr]
            [doric.core :refer [table]]
            [kixi.stats.core :as kixi]
            [manetu.temporal-benchmark.time :as t]
            [manetu.temporal-benchmark.stats :as stats]
            [manetu.temporal-benchmark.utils :refer [round2] :as utils]))

(defn execute-command
  [{:keys [verbose-errors]} f record]
  (log/trace "record:" record)
  (let [start (t/now)]
    (-> (f record)
        (p/then
         (fn [result]
           (log/trace "success for" record)
           {:success true :result result}))
        (p/catch
         (fn [e]
           (let [err-msg (str record ": " (ex-message e) " " (ex-data e))]
             (if verbose-errors
               (log/error err-msg)
               (log/trace err-msg)))
           {:success false :exception e}))
        (p/then
         (fn [result]
           (let [end (t/now)
                 d (t/duration end start)]
             (log/trace record "processed in" d "msecs")
             (assoc result
                    :record record
                    :duration d)))))))

(defn execute-commands
  [{:keys [client-concurrency] :as options} f output-ch input-ch]
  (-> (p/all
       (map
        (fn [_]
          (p/vthread
           (loop []
             (when-let [m (<!! input-ch)]
               (>!! output-ch @(execute-command options f m))
               (recur)))))
        (range client-concurrency)))
      (p/then (fn [_]
                (async/close! output-ch)
                true))))

(defn show-progress
  [{:keys [progress client-concurrency] :as options} n mux]
  (when progress
    (let [ch (async/chan (* 4 client-concurrency))]
      (async/tap mux ch)
      (p/create
       (fn [resolve reject]
         (go-loop [bar (pr/progress-bar n)]
           (if (= (:progress bar) (:total bar))
             (do (pr/print (pr/done bar))
                 (resolve true))
             (do (<! ch)
                 (pr/print bar)
                 (recur (pr/tick bar))))))))))

(defn transduce-promise
  [{:keys [client-concurrency] :as options} n mux xform f]
  (p/create
   (fn [resolve reject]
     (go
       (let [ch (async/chan (* 4 client-concurrency))]
         (async/tap mux ch)
         (let [result (<! (async/transduce xform f (f) ch))]
           (resolve result)))))))

(defn compute-summary-stats
  [options n mux]
  (-> (transduce-promise options n mux (map :duration) stats/summary)
      (p/then (fn [{:keys [dist] :as summary}]
                (-> summary
                    (dissoc :dist)
                    (merge dist)
                    (as-> $ (m/map-vals #(round2 3 (or % 0)) $)))))))

(defn successful?
  [{:keys [success]}]
  (true? success))

(defn failed?
  [{:keys [success]}]
  (false? success))

(defn count-msgs
  [options n mux pred]
  (transduce-promise options n mux (filter pred) kixi/count))

(defn compute-stats
  [options n mux]
  (-> (p/all [(compute-summary-stats options n mux)
              (count-msgs options n mux successful?)
              (count-msgs options n mux failed?)])
      (p/then (fn [[summary s f]] (assoc summary :successes s :failures f)))))

(defn render
  [{:keys [fatal-errors] :as options} {:keys [failures] :as stats}]
  (println (table [:successes :failures :min :mean :stddev :p50 :p90 :p95 :p99 :max :total-duration :rate] [stats]))
  (if (and fatal-errors (pos? failures))
    -1
    0))
