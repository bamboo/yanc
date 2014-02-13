(ns yanc.client.dom-view
  (:require
   [yanc.client.controller :refer [IChatView event-chan event->key run-with]]
   [goog.dom.forms :as gforms]
   [goog.style :as gstyle]
   [goog.dom :as dom]
   [goog.events :as gevents]
   [goog.string :as gstr]))

(enable-console-print!)

(defn dom-view []
  (let [input-box (dom/getElement "input-box")
        output (dom/getElement "output")
        key-ups (event-chan input-box (.-KEYUP gevents/EventType) event->key)]
    (reify IChatView
      (-input-box-value [view]
        (gforms/getValue input-box))
      (-key-ups [view]
        key-ups)
      (-append-html [view format args]
        (let [html (apply gstr/subs format (map gstr/htmlEscape args))
              fragment (dom/htmlToDocumentFragment html)
              p (dom/createDom "p" nil fragment)]
          (dom/append output p)
          (gstyle/scrollIntoContainerView p output false))))))

(run-with (dom-view))
