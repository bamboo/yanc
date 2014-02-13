(ns yanc.client.controller
  (:require-macros
   [cljs.core.async.macros :refer [go-loop]]
   [cljs.core.match.macros :refer [match]])
  (:require
   [goog.dom :as dom]
   [goog.events :as gevents]
   [cljs.core.async :refer [chan put! <! merge map< filter<]]
   [cljs.core.match]
   [cljs.reader :as reader]))

(defn event-chan
  "Creates a channel with events from element el with type event-type
optionally applying function map-event-fn."
  ([el event-type] (event-chan el event-type identity))
  ([el event-type map-event-fn]
     (let [ch (chan)]
       (gevents/listen el event-type #(put! ch (map-event-fn %)))
       ch)))

(def key-codes
  "http://docs.closure-library.googlecode.com/git/closure_goog_events_keynames.js.source.html#line33"
  {38 :up
   40 :down
   13 :enter
   27 :escape})

(defn event->key
  "Maps a js event's keyCode into a known key code symbol or :key-not-found"
  [ev] (get key-codes (.-keyCode ev) :unknown-key))

(defprotocol IChatView
  "A chat view is composed of a input box and its associated channel of key events plus an output view."
  (-input-box-value [view] "current value of the input box")
  (-key-ups [view] "channel with the keys pressed by the user in the main input box")
  (-append-html [view format args] "appends a snippet of html to the output view"))

(def Primus (js* "Primus"))

(def socket-url (.-URL (dom/getDocument)))

(defn socket-chan [socket]
  (let [c (chan)]
    (.on socket "data" #(put! c (reader/read-string %)))
    c))

(defn key->chat-command [^IChatView view key]
  (case key
    :enter [:post-message (-input-box-value view)]
    :escape [:quit]
    :up [:history-up]
    :down [:history-down]))

(defn chat-loop [^IChatView view]
  (let [nick (-input-box-value view)
        write (fn [format & args] (-append-html view format args))
        _ (write "joining <b>%s</b> as <b>%s</b>" socket-url nick)
        socket (.connect Primus socket-url)
        send #(.write socket (pr-str %))
        events (merge [(socket-chan socket)
                       (->> (-key-ups view)
                            (filter< (partial not= :unknown-key))
                            (map< (fn [key] (key->chat-command view key))))])]
    (go-loop []
      (let [e (<! events)]
        (match e
               [:post-message message] (send [:message nick message])
               [:identify] (send [:user-joined nick])
               [:message user message] (write "<b>%s</b> %s" user message)
               [:user-joined user] (write "<b>%s</b> has entered the room." user)
               [:user-left user] (write "<b>%s</b> has left the room." user)
               :else (println "unknown event:" e)))
      (recur))))

(defn run-with [^IChatView view]
  (go-loop []
    (case (<! (-key-ups view))
      :enter (<! (chat-loop view))
      nil)
    (recur)))
