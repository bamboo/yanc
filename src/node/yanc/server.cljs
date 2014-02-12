(ns yanc.server
  (:require-macros
   [cljs.core.async.macros :refer [go-loop]]
   [cljs.core.match.macros :refer [match]])
  (:require
   [cljs.core :as cljs]
   [cljs.core.async :as async :refer [chan put! <! close!]]
   [cljs.core.match]
   [cljs.nodejs :as node :refer [require]]
   [cljs.reader :as reader]))

(defn id [spark]
  (aget spark "id"))

(defn write-edn [socket data]
  (.write socket (pr-str data)))

(defn chat-server-loop [ch primus]
  (let [broadcast (partial write-edn primus)
        nicks (atom {})]
    (go-loop []
      (when-let [e (<! ch)]
        (println e)
        (match e
          [:connection spark]
          (write-edn spark [:identify])

          [:disconnection spark]
          (when-let [nick (get @nicks (id spark))]
            (broadcast [:user-left nick])
            (swap! nicks dissoc nick))

          [:data spark [:user-joined nick] :as message]
          (do
            (broadcast message)
            (swap! nicks assoc (id spark) nick))

          [:data spark message]
          (broadcast message)

          :else
          (println "unknown event:" e))

        (recur)))))

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
