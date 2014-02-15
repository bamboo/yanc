(defproject yanc "0.1.0-SNAPSHOT"
  :description "Yet anoter node/ClojureScript/WebSockets experiment."

  :url "http://github.com/bamboo/yanc"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2156" :scope "provided"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha" :scope "provided"]
                 [org.clojure/core.match "0.2.1"]
                 [org.bodil/cljs-noderepl "0.1.11"]
                 [reagent "0.3.0"]
                 [hiccups "0.3.0"]]

  :plugins [[lein-cljsbuild "1.0.2"]
            [lein-npm "0.3.0"]
            [lein-resource "0.3.3"]]

  :node-dependencies [[ws "0.4.31"]
                      [primus "2.0.1"]
                      [node-static "0.7.3"]]

  :cljsbuild {:builds [{:source-paths ["src/client"]
                        :compiler {:output-to "target/app/resources/js/client.js"
                                   :output-dir "target/app/resources/js"
                                   :optimizations :none
                                   :source-map true}}
                       {:source-paths ["src/node"]
                        :compiler {:output-to "target/app/server.js"
                                   :optimizations :simple
                                   :target :nodejs}}]}

  :resource {:resource-paths ["resources"]
             :target-path "target/app/resources"}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :hooks [leiningen.cljsbuild leiningen.resource])
