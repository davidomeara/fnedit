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

(defn render-style-rule [[k v]]
  (str (name k) ":" v ";"))

(defn render-curly-braces [s]
  (str "{" s "}"))

(defn render-style [m]
  (->> m
       (map render-style-rule)
       (apply str)
       render-curly-braces))

(defn render-widget-style [{:keys [id default hover focus active]}]
  (str "#widget" id (render-style default)
       "#widget" id ":hover" (render-style hover)
       "#widget" id ":focus" (render-style focus)
       "#widget" id ":active" (render-style active)))

(defonce widget-id (atom 0))

(defn button-style [id theme style-options enabled?]
  {:id      id
   :default (merge
              {:display "inline-block"
               :cursor  "pointer"
               :margin  "2px"
               :padding "2px 5px 4px 5px"
               :color   (:color theme)
               :border  "1px solid transparent"
               :outline 0}
              (:default style-options)
              (if enabled?
                {}
                {:cursor "default"
                 :color  (:border-b theme)
                 :border "1px solid transparent"}))
   :hover   (if enabled?
              (merge
                {:color  (:active theme)
                 :border (str "1px solid " (:active theme))}
                (:hover style-options))
              {})
   :focus   (if enabled?
              (merge
                {:color  (:active theme)
                 :border (str "1px solid " (:active theme))}
                (:focus style-options))
              {})
   :active  (if enabled?
              (merge
                {:color            (:active-color theme)
                 :background-color (:active theme)
                 :border           (str "1px solid " (:active theme))}
                (:active style-options))
              {})})

; style channel key title
(defn button
  "required
    channel, theme, key, title, button-options
   button-options [key=default]
    :tab-index=-1
    :style=nil
    :enabled?=true
    :status=nil"
  [_ _ _ _ _]
  (let [id (swap! widget-id inc)]
    (fn [channel theme key title button-options]
      (let [options (merge {:tab-index -1 :enabled? true} button-options)
            status (fn [k]
                     (fn []
                       (when (and (:enabled? options) (:status options))
                         (put! channel [k (:status options)])
                         nil)))
            action (fn [] (when (:enabled? options) (put! channel [key])) nil)]
        [:a
         {:id             (str "widget" id)
          :tabIndex       (:tab-index options)
          :on-focus       (status :focus)
          :on-blur        (status :blur)
          :on-mouse-enter (status :hover)
          :on-mouse-leave (status :unhover)
          :on-mouse-up    action
          :on-key-up      (key-down (fn [_] (action)))}
         title
         [:style {:style {:display "none"}}
          (render-widget-style
            (button-style id theme (:style options) (:enabled? options)))]]))))

(def standard-button-style
  {:display    "inline-block"
   :width      "100px"
   :text-align "center"})

(def icon-style {:style {:margin-right "5px"}})

(defn standard-button [channel theme key title options]
  [button
   channel
   theme
   key
   title
   (update-in options [:style :default] merge standard-button-style)])

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
