(ns yanc.client.dom-view
  (:require
   [yanc.client.controller :as controller]
   [cljs.core.async :refer [chan]]
   [goog.style :as gstyle]
   [goog.dom :as dom]
   [goog.events :as gevents]
   [hiccups.runtime :as hiccups]))

(defn dom-view [input-box-id output-div-id]
  (let [input-box (dom/getElement input-box-id)
        output (dom/getElement output-div-id)
        inputs (chan)]
    (gevents/listen input-box (.-KEYUP gevents/EventType) #(controller/on-key-up % inputs))
    (reify controller/IChatView
      (-inputs [view]
        inputs)
      (-append-html [view snippet]
        (let [html (hiccups/render-html snippet)
              fragment (dom/htmlToDocumentFragment html)
              p (dom/createDom "p" nil fragment)]
          (dom/append output p)
          (gstyle/scrollIntoContainerView p output false))))))

(controller/run-with (dom-view "input-box" "output"))
