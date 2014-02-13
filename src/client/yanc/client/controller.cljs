(ns yanc.client.controller
  (:require-macros
   [cljs.core.async.macros :refer [go-loop]]
   [cljs.core.match.macros :refer [match]])
  (:require
   [goog.dom :as dom]
   [cljs.core.async :refer [chan put! <! merge map< filter<]]
   [cljs.core.match]
   [cljs.reader :as reader]))

(def key-codes
  "http://docs.closure-library.googlecode.com/git/closure_goog_events_keynames.js.source.html#line33"
  {38 :up
   40 :down
   13 :enter
   27 :escape})

(defn event->key
  "Maps a js event's keyCode into a known key code symbol or :key-not-found"
  [ev] (get key-codes (.-keyCode ev) :unknown-key))

(defn on-key-up [e inputs]
  "Handles a KEYUP event by putting the appropriate input event into the inputs channel."
  (when (= :enter (event->key e))
    (put! inputs [:input (-> e .-target .-value)])))

(defprotocol IChatView
  (-inputs [view]
    "Channel where to take input commands from. Valid commands are [:input \"mynick\"] and [:quit].")
  (-append-html [view snippet]
    "Appends a snippet of html to the output view (snippet is in hiccups format: [:b \"a snippet\"]"))

(def Primus (js* "Primus"))

(def socket-url (.-URL (dom/getDocument)))

(defn socket-chan [socket]
  (let [c (chan)]
    (.on socket "data" #(put! c (reader/read-string %)))
    c))

(defn chat-loop [^IChatView view nick]
  (let [write #(-append-html view (apply vector :p %))
        _ (write ["joining " [:b socket-url] " as " [:b nick]])
        socket (.connect Primus socket-url)
        send #(.write socket (pr-str %))
        events (merge [(socket-chan socket)
                       (-inputs view)])]
    (go-loop []
      (let [e (<! events)]
        (match e
               [:input message] (send [:message nick message])
               [:identify] (send [:user-joined nick])
               [:message user message] (write [[:b user] " " message])
               [:user-joined user] (write [[:b user] " has entered the room."])
               [:user-left user] (write [[:b user] " has left the room."])
               :else (println "unknown event:" e)))
      (recur))))

(defn run-with [^IChatView view]
  (go-loop []
    (match (<! (-inputs view))
           [:input nick] (<! (chat-loop view nick))
           :else nil)
    (recur)))

(enable-console-print!)
