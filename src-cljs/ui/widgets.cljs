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
        blur (fn [] (when-let [s (:status options)] (put! channel [:blur])) nil)
        unhover (fn [] (when-let [s (:status options)] (put! channel [:unhover])))
        action (fn [] (put! channel (:action options)) nil)]
    (if (:enabled? options)
      [:a.unselectable.button.font
       {:tabIndex (:tab-index options)
        :on-focus (fn [] (when-let [s (:status options)] (put! channel [:focus s])) nil)
        :on-blur blur
        :on-mouse-enter (fn [] (when-let [s (:status options)] (put! channel [:hover s])) nil)
        :on-mouse-leave (fn [] (unhover) (blur))
        :on-click (fn [] (unhover) (blur) (action))
        :on-key-down (key-down (fn [] (unhover) (blur) (action)))
        :style (merge (:style options) {:display "inline-block" :cursor "pointer"})}
       title]
      [:a.unselectable.button.font
       {:tabIndex (:tab-index options)
        :style (merge (:style options) {:display "inline-block"
                                        :color "#999"
                                        :border "1px solid transparent"})}
       title])))

(def standard-widget-style
  {:display "inline-block"
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
