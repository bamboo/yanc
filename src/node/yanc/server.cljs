(ns yanc.server
  (:require [cljs.core :as cljs]
            [cljs.nodejs :as node :refer [require]]))

(defn on-spark [spark]
  (.on spark "data" #(println "received " %)))

(defn handler [req res resources]
  (println "handler")
  (->
   (.addListener req "end" #(.serve resources req res))
   (.resume)))

(defn start [& {root :root :or {root "./resources"}}]
  (let [path (require "path")
        root (.resolve path root)
        StaticServer (-> (require "node-static") .-Server)
        resources (new StaticServer root)
        http (require "http")
        server (.createServer http #(handler %1 %2 resources))
        Primus (require "primus")
        primus (new Primus server)]
    (println "root is" root)
    (.listen server 8080)
    (.on primus "connection" on-spark)
    server))

(defn main [& args]
  (start))

;(def s (start :root "target/app/resources"))
;(.close s)

(enable-console-print!)
(set! *main-cli-fn* main)
