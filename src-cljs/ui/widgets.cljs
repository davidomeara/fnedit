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

(defn button-style [style enabled? {:keys [active? hover? focus?]}]
  (if enabled?
    (if active?
      {:color (:active-color style)
       :background-color (:active style)
       :border (str "1px solid " (:active style))}
      (if (or hover? focus?)
        {:color (:active style)
         :border (str "1px solid " (:active style))}
        {}))
    {:cursor "default"
     :color (:border-b style)
     :border "1px solid transparent"}))

; style channel key title
(defn button
  "required
    style, channel, title
   optional [key=default]
    :tab-index=-1
    :style=nil
    :enabled?=true
    :status=nil           When not nil, set as focus using the channel."
  [_ _ _ _ _]
  (let [state (reagent/atom {:active? false :hover? false :focus? false})]
    (fn [channel theme key title button-options]
      (let [options (merge {:tab-index -1 :enabled? true} button-options)
            activate (fn [] (when (:enabled? options) (swap! state assoc :active? true)) nil)
            disactivate (fn [] (swap! state assoc :active? false) nil)
            action (fn [] (when (:enabled? options) (put! channel [key])) nil)]
        [:a.unselectable
         {:tabIndex (:tab-index options)
          :on-focus (fn []
                      (when-let [s (:status options)]
                        (put! channel [:focus s]))
                      (swap! state assoc :focus? true)
                      nil)
          :on-blur (fn []
                     (when-let [s (:status options)]
                       (put! channel [:blur]))
                     (swap! state assoc :focus? false)
                     (disactivate)
                     nil)
          :on-mouse-enter (fn []
                            (when-let [s (:status options)]
                              (put! channel [:hover s]))
                            (swap! state assoc :hover? true)
                            nil)
          :on-mouse-down activate
          :on-mouse-leave (fn []
                            (when-let [s (:status options)]
                              (put! channel [:unhover]))
                            (swap! state assoc :hover? false)
                            (disactivate)
                            nil)
          :on-mouse-up (fn [] (action) (disactivate))
          :on-key-down activate
          :on-key-up (key-down (fn [_] (action) (disactivate)))
          :style (merge
                   {:display "inline-block"
                    :cursor "pointer"
                    :margin "2px"
                    :padding "2px 5px 4px 5px"
                    :color (:color theme)
                    :border "1px solid transparent"
                    :outline 0}
                   (button-style theme (:enabled? options) @state)
                   (:style options))}
         [:div {:style {:display "none"}} (debug/stringify @state)]
         title]))))

(def standard-widget-style
  {:display "inline-block"
   :width "100px"})

(def standard-button-style
  (merge standard-widget-style
    {:text-align "center"}))

(def icon-style {:style {:margin-right "5px"}})

(defn standard-button [channel theme key title options]
  [button
   channel
   theme
   key
   title
   (merge options {:style (merge (:style options) standard-button-style)})])

(defn positive-button [channel theme key caption options]
  [standard-button
   channel
   theme
   key
   [:span [:i.icon.ion-checkmark icon-style] caption]
   options])

(defn negative-button [channel theme key caption options]
  [standard-button
   channel
   theme
   key
   [:span [:i.icon.ion-close icon-style] caption]
   options])
