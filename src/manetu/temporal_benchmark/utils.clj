;; Copyright © Manetu, Inc.  All rights reserved

(ns manetu.temporal-benchmark.utils
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [slingshot.slingshot :refer [throw+ try+]]
            [temporal.client.core :as c]
            [temporal.tls :as tls]))

(def tool-name "temporal-benchmark")

(defn version [] (str tool-name " version: v" (System/getProperty "temporal-benchmark.version")))

(defn prep-usage [msg] (->> msg flatten (string/join \newline)))

(def usage-preamble (str "Usage: " tool-name " [global-options]"))

(defn subcommand-usage
  [subcommand description global-summary local-summary]
  (prep-usage [(str usage-preamble " " subcommand " [options]")
               ""
               description
               ""
               "Subcommand Options:"
               local-summary
               ""
               "Global Options:"
               global-summary]))

(defn exit [status msg & args]
  (throw+ {:type ::exit :status status :msg (apply str msg args)}))

(defn help-exit [summary]
  (exit 0 summary))

(defn error-exit [error-msg summary]
  (exit -1 (str "Error: " error-msg "\n\n") summary))

(defn round2
  "Round a double to the given precision (number of significant digits)"
  [precision ^double d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(defn new-ssl-context [{:keys [temporal-ca] :as global-options}]
  (-> {}
      (cond-> temporal-ca (assoc :ca-path temporal-ca))
      (tls/new-ssl-context)))

(defn create-client [{:keys [temporal-target temporal-namespace temporal-tls] :as global-options}]
  (c/create-client (-> {:target temporal-target
                        :namespace temporal-namespace}
                       (cond-> temporal-tls (assoc :ssl-context (new-ssl-context global-options))))))

(defn exec-command
  [{:keys [command description options-spec fn]} global-summary global-options args]
  (let [{{:keys [help] :as local-options} :options
         :keys [errors summary]} (parse-opts args (cons ["-h" "--help"] options-spec))
        summary (subcommand-usage command description global-summary summary)]
    (cond

      help
      (help-exit summary)

      (not= errors nil)
      (error-exit (string/join errors) summary)

      :else
      (let [options (merge global-options local-options)
            client (create-client global-options)]
        (fn options client)))))
