(ns ui.widgets
  (:require [reagent.core :as reagent]
            [ui.events :as events]
            [ui.debug :as debug]))

(defn key-down [f]
  (fn [e]
    (when (= (.-which e) 13)
      (f))
    nil))

(defn button [title tabindex style enabled? on-click-fn]
  (if enabled?
    [:a.unselectable.widget-behavior.widget-color.font
     {:tabIndex tabindex
      :on-click (fn [] (on-click-fn) nil)
      :on-key-down (key-down on-click-fn)
      :style (merge style {:display "inline-block"
                           :cursor "pointer"})}
     title]
    [:a.unselectable.widget-behavior.disabled-widget-color.font
     {:tabIndex tabindex
      :style (merge style {:display "inline-block"})}
     title]))

(def standard-widget-style
  {:margin "2px"
   :display "inline-block"
   :width "100px"})

(def standard-button-style
  (merge standard-widget-style
    {:text-align "center"}))

(def icon-style {:style {:margin-right "5px"}})

(defn positive-button [caption tabindex enabled? on-click-fn]
  [button
   [:span [:i.icon.ion-checkmark icon-style] caption]
   tabindex
   standard-button-style
   enabled?
   on-click-fn])

(defn negative-button [caption tabindex enabled? on-click-fn]
  [button
   [:span [:i.icon.ion-close icon-style] caption]
   tabindex
   standard-button-style
   enabled?
   on-click-fn])
