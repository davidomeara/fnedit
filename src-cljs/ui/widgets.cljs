(ns ui.widgets
  (:require [cljs.core.async :refer [put!]]
            [reagent.core :as reagent]
            [ui.events :as events]
            [ui.debug :as debug]))

(defn key-down [f]
  (fn [e]
    (when (= (.-which e) 13)
      (f))
    nil))

(defn button
  "required
    channel, title
   optional [key=default]
    :tab-index=-1
    :style=nil
    :enabled?=true
    :action=nil           When not nil, sent to channel on click or focus enter.
    :status=nil           When not nil, set as focus using the channel."
  [channel title button-options]
  (let [options (merge {:tab-index -1 :enabled? true} button-options)
        put #(put! channel (:action options))]
    (if (:enabled? options)
      [:a.unselectable.widget-behavior.widget-color.font
       {:tabIndex (:tab-index options)
        :on-click (fn [e] (put) nil)
        :on-key-down (key-down put)
        :style (merge (:style options) {:display "inline-block" :cursor "pointer"})}
       title]
      [:a.unselectable.widget-behavior.disabled-widget-color.font
       {:tabIndex (:tab-index options)
        :style (merge (:style options) {:display "inline-block"})}
       title])))

(def standard-widget-style
  {:margin "2px"
   :display "inline-block"
   :width "100px"})

(def standard-button-style
  (merge standard-widget-style
    {:text-align "center"}))

(def icon-style {:style {:margin-right "5px"}})

(defn standard-button [channel title options]
  [button
   channel
   title
   (merge options {:style (merge (:style options) standard-button-style)})])

(defn positive-button [channel caption options]
  [standard-button
   channel
   [:span [:i.icon.ion-checkmark icon-style] caption]
   options])

(defn negative-button [channel caption options]
  [standard-button
   channel
   [:span [:i.icon.ion-close icon-style] caption]
   options])
