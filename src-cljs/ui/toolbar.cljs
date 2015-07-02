(ns ui.toolbar
  (:require [cljs.core.async :refer [put!]]
            [reagent.core :as reagent]
            [ui.events :as events]
            [ui.widgets :refer [button]]
            [ui.debug :as debug]))

(def button-style
  {:default {:flex-grow   0
             :flex-shrink 0
             :margin      0
             :text-align  "center"
             :line-height "24px"
             :width       "24px"
             :height      "24px"}
   :hover   {:border "1px solid transparent"}
   :focus   {:border "1px solid transparent"}})

(def icon-style {:style {:font-size "18px"}})

(defn toolbar [channel theme opened]
  [:div
   {:style {:flex-grow        0
            :flex-shrink      0
            :display          "flex"
            :flex-direction   "row"
            :background-color (:background theme)
            :border-bottom    (str "1px solid " (:border-b theme))}}

   [:div {:style {:display         "flex"
                  :flex-direction  "row"
                  :justify-content "flex-start"}}
    [button
     channel
     theme
     :open-root-directory
     [:i.icon.ion-ios-folder-outline icon-style]
     {:style  button-style
      :status "Open folder (Ctrl+O)"}]

    [button
     channel
     theme
     :new
     [:i.icon.ion-ios-compose-outline icon-style]
     {:style  button-style
      :status "New file (Ctrl+N)"}]]

   [:div {:style {:display         "flex"
                  :flex-direction  "row"
                  :justify-content "flex-end"
                  :flex-grow       1}}
    [button
     channel
     theme
     :delete
     [:i.icon.ion-ios-trash-outline icon-style]
     {:style    button-style
      :enabled? (:path opened)
      :status   "Delete opened file"}]

    [button
     channel
     theme
     :save
     [:i.icon.ion-ios-download-outline icon-style]
     {:style    button-style
      :enabled? (:dirty? opened)
      :status   "Save opened file (Ctrl+S)"}]

    [button
     channel
     theme
     :evaluate-form
     [:span "(...)"]
     {:style    button-style
      :enabled? (:cursor-selection opened)
      :status   "Eval top level forms adjacent and within the selection (Ctrl+Enter)"}]

    [button
     channel
     theme
     :evaluate-script
     [:i.icon.ion-ios-arrow-thin-down icon-style]
     {:style    button-style
      :enabled? opened
      :status   "Eval all forms in file (Ctrl+Shift+Enter)"}]]])
