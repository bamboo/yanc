(ns yanc.client
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [cljs.core.match.macros :refer [match]])
  (:require
   [goog.dom.forms :as gforms]
   [goog.style :as gstyle]
   [goog.dom :as dom]
   [goog.events :as gevents]
   [goog.string :as gstr]
   [cljs.core.async :as async :refer [chan put! <! pipe unique merge map< filter< alts!]]
   [cljs.core.match]))

(enable-console-print!)

(def input-box (dom/getElement "input-box"))

(def output (dom/getElement "output"))

(defn append-html [s & args]
  (let [html (apply gstr/subs s (map gstr/htmlEscape args))
        fragment (dom/htmlToDocumentFragment html)
        p (dom/createDom "p" nil fragment)]
    (dom/append output p)
    (gstyle/scrollIntoContainerView p output false)))

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

(defn alert [msg]
  (.alert js/window msg))

(def key-ups (event-chan input-box (.-KEYUP gevents/EventType) event->key))

(declare chat-loop post-message key->chat-command)

(defn main []
  (go-loop [key (<! key-ups)]
    (case key
      :enter (<! (chat-loop))
      nil)
    (recur (<! key-ups))))

(defn chat-loop []
  (append-html "joining...")
  (let [socket nil
        socket-chan (chan)
        events (merge [(map< key->chat-command (filter< (partial not= :unknown-key) key-ups))
                       socket-chan])]
    (go-loop [e (<! events)]
      (match e
       [:message-posted message] (post-message socket message)
       [:message-received user message] (append-html "<b>%s</b> %s" user message)
       [:user-joined user] (append-html "<b>%s</b> has entered the room." user)
       [:user-left user] (append-html "<b>%s</b> has left the room." user)
       :else (println "unknown event:" e))
      (recur (<! events)))))

(defn key->chat-command [key]
  (case key
    :enter [:message-posted (gforms/getValue input-box)]
    :escape [:quit]
    :up [:history-up]
    :down [:history-down]))

(defn post-message [socket message]
  (append-html "posting <b>%s</b>" message))

(main)
