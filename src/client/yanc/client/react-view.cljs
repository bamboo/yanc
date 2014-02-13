(ns yanc.client.react-view
  (:require
   [cljs.core.async :refer [chan put!]]
   [yanc.client.controller :as controller]
   [reagent.core :as reagent :refer [atom]]))

(def messages (atom []))

(def inputs (chan))

(defn view []
  [:div.container

   [:div.page-header
    [:h1 "Yet Another Node Chat (react style)"]]

   [:input.form-control {:type "text"
                         :placeholder "type your nick and press enter to join"
                         :on-key-up #(controller/on-key-up % inputs)}]

   [:div.well {:style {"overflow-y" "auto"
                       "height" "340px"}}
    (apply vector :div @messages)]])

(reagent/render-component [view] (.-body js/document))

(controller/run-with
 (let [key-ups (chan)]
   (reify controller/IChatView
     (-inputs [view]
       inputs)
     (-append-html [view snippet]
       (swap! messages conj snippet)))))
