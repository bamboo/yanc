(ns yanc.server
  (:require-macros
   [cljs.core.async.macros :refer [go-loop]]
   [cljs.core.match.macros :refer [match]])
  (:require
   [cljs.core :as cljs]
   [cljs.core.async :refer [chan put! <! close!]]
   [cljs.core.match]
   [cljs.nodejs :as node :refer [require]]
   [cljs.reader :as reader]))

(defn write-edn
  "writes data to a socket in edn format."
  [socket data]
  (. socket write (pr-str data)))

(defn chat-server-loop
  "takes chat events from the `events` channel broadcasting messages to the primus server `primus`."
  [events primus]
  (let [broadcast (partial write-edn primus)
        nicks (atom {})]
    (go-loop []
      (when-let [e (<! events)]
        (println e)
        (match e
          [:connection spark] ;; a new connection has been made, ask for its identity (nick)
          (write-edn spark [:identify])

          [:disconnection spark]
          (when-let [nick (get @nicks (.-id spark))]
            (broadcast [:user-left nick])
            (swap! nicks dissoc nick))

          [:data spark [:user-joined nick] :as message]
          (do
            (broadcast message)
            (swap! nicks assoc (.-id spark) nick))

          [:data spark message]
          (broadcast message)

          :else
          (println "unknown event:" e))
        (recur)))))

(defn chat-events-channel-for
  "maps primus websocket connection and data events to higher level chat events served through the returned channel."
  [primus]
  (let [chat-events (chan 1)
        publish-event #(put! chat-events %)]

    (.on primus "connection"
         (fn [spark]
           (do
             (publish-event [:connection spark])
             (.on spark "data" #(publish-event [:data spark (reader/read-string %)])))))
    (.on primus "disconnection" #(publish-event [:disconnection %]))

    chat-events))

(defn default-request-handler
  "serves requests for static resources from resources."
  [req res resources]
  (->
   (. req addListener "end" #(. resources serve req res))
   (.resume)))

(defn start
  "starts a http server that serve resources from resources-path and accepts primus websocket connections."
  [& [resources-path]]
  (let [path (require "path")
        resources-path (. path resolve (or resources-path "./resources"))
        StaticServer (-> (require "node-static") .-Server)
        resources (new StaticServer resources-path)
        http (require "http")
        server (. http createServer #(default-request-handler %1 %2 resources))
        Primus (require "primus")
        primus (new Primus server)
        chat-events (chat-events-channel-for primus)]

    (println "resources path is" resources-path)
    (.on server "close" #(close! chat-events))
    (chat-server-loop chat-events primus)
    (. server listen 8080)

    server))

(defn main [& args]
  (apply start args))

;(def s (start "target/app/resources"))
;(.close s)

(enable-console-print!)
(set! *main-cli-fn* main)
