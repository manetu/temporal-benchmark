(defproject manetu/temporal-benchmark "0.0.1-SNAPSHOT"
  :description "A utility to measure performance of a temporal cluster"
  :url "https://github.com/manetu/temporal-benchmark"
  :plugins [[lein-cljfmt "0.9.0"]
            [lein-kibit "0.1.8"]
            [lein-bikeshed "0.5.2"]
            [lein-cloverage "1.2.3"]
            [jonase/eastwood "1.3.0"]
            [lein-bin "0.3.5"]]
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/core.async "1.6.681"]
                 [org.clojure/tools.cli "1.1.230"]
                 [com.taoensso/encore "3.150.0"]
                 [com.taoensso/timbre "6.7.1"]
                 [com.fzakaria/slf4j-timbre "0.4.1"]
                 [org.slf4j/jul-to-slf4j "2.0.16"]
                 [org.slf4j/jcl-over-slf4j "2.0.16"]
                 [org.slf4j/log4j-over-slf4j "2.0.16"]
                 [progrock "0.1.2"]
                 [doric "0.9.0"]
                 [kixi/stats "0.5.7"]
                 [cheshire "5.13.0"]
                 [slingshot "0.12.2"]
                 [io.github.manetu/temporal-sdk "1.3.2"]]
  :main ^:skip-aot manetu.temporal-benchmark.main
  :target-path "target/%s"
  :uberjar-name "app.jar"
  :jvm-opts ["-server"]

  :manifest {"Multi-Release" true}                          ;; needed so that we use the right version of temporal vthreads

  :bin {:name "temporal-benchmark"
        :bootclasspath false}

  :eastwood {:add-linters [:unused-namespaces]
             :exclude-linters [:deprecations :suspicious-expression :local-shadows-var :unused-meta-on-macro :reflection]}

  ;; nREPL by default starts in the :main namespace, we want to start in `user`
  ;; because that's where our development helper functions like (refresh) live.
  :repl-options {:init-ns user}

  :profiles {:dev {:dependencies [[clj-http "3.13.0"]
                                  [org.clojure/tools.namespace "1.5.0"]]}
             :uberjar {:aot :all}})
