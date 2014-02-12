(ns yanc.server
  (:require-macros
   [cljs.core.async.macros :refer [go-loop]])
  (:require
   [cljs.core :as cljs]
   [cljs.core.async :as async :refer [chan put! <! merge map< filter< close!]]
   [cljs.nodejs :as node :refer [require]]
   [cljs.reader :as reader]))

(defn chat-server-loop [ch primus]
  (go-loop []
    (when-let [e (<! ch)]
      (println e)
      (case (first e)
        :connection (println "connection:" (aget (second e) "id"))
        :disconnection (println "disconnection:" (aget (second e) "id"))
        :data (let [[_ spark message] e] (.write primus (pr-str message)))
        :else (println "unknown event:" e))
      (recur))))

(defn handler [req res resources]
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
        primus (new Primus server)
        ch (chan 1)]
    (println "root is" root)

    ;; map socket events to higher level commands
    (.on primus "connection"
         (fn [spark]
           (do
             (put! ch [:connection spark])
             (.on spark "data" #(put! ch [:data spark (reader/read-string %)])))))
    (.on primus "disconnection" #(put! ch [:disconnection %]))
    (.on server "close" #(close! ch))

    (chat-server-loop ch primus)

    (.listen server 8080)
    server))

(defn main [& args]
  (start))

;(def s (start :root "target/app/resources"))
;(.close s)

(enable-console-print!)
(set! *main-cli-fn* main)
