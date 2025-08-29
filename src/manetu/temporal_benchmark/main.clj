;; Copyright © Manetu, Inc.  All rights reserved

(ns manetu.temporal-benchmark.main
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [taoensso.timbre :as log]
            [slingshot.slingshot :refer [throw+ try+]]
            [manetu.temporal-benchmark.commands :as commands]
            [manetu.temporal-benchmark.utils :refer [prep-usage tool-name usage-preamble
                                                     help-exit error-exit exit version] :as utils])
  (:gen-class))

(defn set-logging
  [level]
  (log/set-config!
   {:level level
    :ns-whitelist  ["manetu.*"]
    :appenders
    {:custom
     {:enabled? true
      :async false
      :fn (fn [{:keys [timestamp_ msg_ level] :as data}]
            (binding [*out* *err*]
              (println (force timestamp_) (string/upper-case (name level)) (force msg_))))}}}))

(def log-levels #{:trace :debug :info :error})
(defn print-loglevels []
  (str "[" (string/join ", " (map name log-levels)) "]"))
(def loglevel-description
  (str "Select the logging verbosity level from: " (print-loglevels)))

(def options-spec
  [["-h" "--help"]
   ["-v" "--version" "Print version info and exit"]
   [nil "--temporal-target HOSTPORT" "The host:port of the Temporal cluster"
    :default "localhost:7233"]
   [nil "--temporal-taskqueue QUEUENAME" "The temporal taskqueue to use"
    :default "temporal-benchmark"]
   ["-l" "--log-level LEVEL" loglevel-description
    :default :info
    :parse-fn keyword
    :validate [log-levels (str "Must be one of " (print-loglevels))]]])

(defn usage [options-summary]
  (prep-usage (-> [(str usage-preamble " subcommand [subcommand-options]")
                   ""
                   "Subcommands:"]
                  (concat
                   (commands/render-description))
                  (concat
                   [""
                    "Global Options:"
                    options-summary
                    ""
                    (str "Use '" tool-name " <subcommand> -h' for subcommand specific help")]))))

(defn -app
  [& args]
  (let [{{:keys [help log-level url token] :as global-options} :options
         global-summary :summary
         :keys [arguments errors]}
        (parse-opts args options-spec :in-order true)
        subcommand (first arguments)
        usage-summary (usage global-summary)]
    (try+
     (cond

       help
       (help-exit usage-summary)

       (not= errors nil)
       (error-exit (string/join errors) usage-summary)

       (:version global-options)
       (exit 0 (version))

       (string/blank? subcommand)
       (error-exit "subcommand required" usage-summary)

       :else
       (do
         (set-logging log-level)
         (if-let [exec-fn (commands/get-handler subcommand)]
           (or (exec-fn global-summary global-options arguments)
               0)
           (error-exit (str "unknown subcommand: \"" subcommand "\"") (usage global-summary)))))
     (catch [:type ::utils/exit] {:keys [status msg]}
       (println msg)
       status))))

(defn -main
  [& args]
  (System/exit (apply -app args)))
