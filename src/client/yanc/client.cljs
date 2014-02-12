(ns yanc.client
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [goog.dom :as dom]
   [goog.events :as events]
   [cljs.core.async :as async :refer [chan put! <! pipe unique merge map< filter< alts!]]))

(enable-console-print!)

(def input-box (dom/getElement "input-box"))

(defn event-chan
  "Creates a channel with events from element el with type event-type
optionally applying function map-event-fn."
  ([el event-type] (event-chan el event-type identity))
  ([el event-type map-event-fn]
     (let [ch (chan)]
       (events/listen el event-type #(put! ch (map-event-fn %)))
       ch)))

(def key-codes
  "http://docs.closure-library.googlecode.com/git/closure_goog_events_keynames.js.source.html#line33"
  {38 :up
   40 :down
   13 :enter})

(defn event->key
  "Maps a js event's keyCode into a known key code symbol or :key-not-found"
  [ev] (get key-codes (.-keyCode ev) :key-not-found))

(defn alert [msg]
  (.alert js/window msg))

(defn main []
  (let [key-ups (event-chan input-box (.-KEYUP events/EventType) event->key)]
    (go-loop [e (<! key-ups)]
      (case e
        :enter (alert "soon!")
        (println "unsupported event" e))
      (recur (<! key-ups)))))

(main)
