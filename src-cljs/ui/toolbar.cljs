(ns ui.toolbar
  (:require [cljs.core.async :refer [put!]]
            [reagent.core :as reagent]
            [ui.events :as events]
            [ui.widgets :refer [button]]
            [ui.debug :as debug]))

(def button-style
  {:flex-grow 0
   :flex-shrink 0
   :margin 0
   :border "1px solid transparent"
   :text-align "center"
   :line-height "24px"
   :width "24px"
   :height "24px"})

(def icon-style {:style {:font-size "18px"}})

(defn toolbar [style opened channel]
  [:div.unselectable
   {:style {:flex-grow 0
            :flex-shrink 0
            :display "flex"
            :flex-direction "row"
            :background-color (:background style)
            :border-bottom (str "1px solid " (:border-b style))}}

   [:div {:style {:display "flex"
                  :flex-direction "row"
                  :justify-content "flex-start"}}
    [button
     channel
     [:i.icon.ion-ios-folder-outline icon-style]
     {:style button-style
      :action [:open-root-directory]
      :status "Open folder (Ctrl+O)"}]

    [button
     channel
     [:i.icon.ion-ios-compose-outline icon-style]
     {:style button-style
      :action [:new]
      :status "New file (Ctrl+N)"}]]

   [:div {:style {:display "flex"
                  :flex-direction "row"
                  :justify-content "flex-end"
                  :flex-grow 1}}
    [button
     channel
     [:i.icon.ion-ios-trash-outline icon-style]
     {:style button-style
      :enabled? (:path opened)
      :action [:delete]
      :status "Delete opened file"}]

    [button
     channel
     [:i.icon.ion-ios-download-outline icon-style]
     {:style button-style
      :enabled? (:dirty? opened)
      :action [:save]
      :status "Save opened file (Ctrl+S)"}]

    [button
     channel
     [:span "(...)"]
     {:style button-style
      :enabled? (:cursor-selection opened)
      :action [:evaluate-form]
      :status "Eval top level forms that fall within the selection (Ctrl+Space)"}]

    [button
     channel
     [:i.icon.ion-ios-arrow-thin-down icon-style]
     {:style button-style
      :enabled? opened
      :action [:evaluate-script]
      :status "Eval all forms in file (Ctrl+Shift+Space)"}]]])
