(ns ui.toolbar
  (:require [cljs.core.async :refer [put!]]
            [reagent.core :as reagent]
            [ui.events :as events]
            [ui.widgets :refer [button]]
            [ui.debug :as debug]))

(def button-style
  {:flex-grow 0
   :flex-shrink 0
   :margin "2px"
   :border 0
   :text-align "left"
   :line-height "24px"})

(def toolbar-style
  {:style {:display "flex"
           :flex-direction "row"
           :align-items "center"}})

(def icon-style {:style {:margin-right "5px"
                         :font-size "18px"}})

(defn toolbar [opened channel]
  [:div.unselectable
   {:style {:flex-grow 0
            :flex-shrink 0
            :display "flex"
            :flex-direction "row"
            :background-color "#f5f2f1"
            :border-bottom "1px solid #b6b6b7"}}

   [button
    channel
    [:span toolbar-style [:i.icon.ion-ios-folder-outline icon-style] "Open"]
    {:style button-style
     :action [:open-root-directory]}]

   [button
    channel
    [:span toolbar-style [:i.icon.ion-ios-compose-outline icon-style] "New"]
    {:style button-style
     :action [:new]}]

   [button
    channel
    [:span toolbar-style [:i.icon.ion-ios-trash-outline icon-style] "Delete"]
    {:style button-style
     :enabled? (:path opened)
     :action [:delete]}]

   [button
    channel
    [:span toolbar-style [:i.icon.ion-ios-download-outline icon-style] "Save"]
    {:style button-style
     :enabled? (:dirty? opened)
     :action [:save]}]

   [button
    channel
    [:span toolbar-style [:span {:style {:margin-right "5px"}} "(...)"] "Eval selection"]
    {:style button-style
     :enabled? (:cursor-selection opened)
     :action [:evaluate-form]}]

   [button
    channel
    [:span toolbar-style [:i.icon.ion-ios-arrow-thin-down icon-style] "Eval file"]
    {:style button-style
     :enabled? opened
     :action [:evaluate-script]}]])
