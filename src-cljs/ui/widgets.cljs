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
  [style channel title button-options]
  (let [options (merge {:tab-index -1 :enabled? true} button-options)
        action (fn [] (put! channel (:action options)) nil)]
    (if (:enabled? options)
      [:a.unselectable.button
       {:tabIndex (:tab-index options)
        :on-focus (fn [] (when-let [s (:status options)] (put! channel [:focus s])) nil)
        :on-blur (fn [] (when-let [s (:status options)] (put! channel [:blur])) nil)
        :on-mouse-enter (fn [] (when-let [s (:status options)] (put! channel [:hover s])) nil)
        :on-mouse-leave (fn [] (when-let [s (:status options)] (put! channel [:unhover])))
        :on-click action
        :on-key-down (key-down action)
        :style (merge (:style options) {:display "inline-block" :cursor "pointer"})}
       title]
      [:a.unselectable.button
       {:tabIndex (:tab-index options)
        :style (merge (:style options) {:display "inline-block"
                                        :color (:border-b style)
                                        :border "1px solid transparent"})}
       title])))

(def standard-widget-style
  {:display "inline-block"
   :width "100px"})

(def standard-button-style
  (merge standard-widget-style
    {:text-align "center"}))

(def icon-style {:style {:margin-right "5px"}})

(defn standard-button [style channel title options]
  [button
   style
   channel
   title
   (merge options {:style (merge (:style options) standard-button-style)})])

(defn positive-button [style channel caption options]
  [standard-button
   style
   channel
   [:span [:i.icon.ion-checkmark icon-style] caption]
   options])

(defn negative-button [style channel caption options]
  [standard-button
   style
   channel
   [:span [:i.icon.ion-close icon-style] caption]
   options])
