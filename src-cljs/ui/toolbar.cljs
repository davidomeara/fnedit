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

(defn toolbar [opened out]
  [:div.unselectable
   {:on-context-menu #(events/stop-event %)
    :on-focus #(-> % .-nativeEvent .-target .blur)
    :style {:flex-grow 0
            :flex-shrink 0
            :display "flex"
            :flex-direction "row"
            :background-color "#f5f2f1"
            :border-bottom "1px solid #b6b6b7"}}

   [button
    [:span toolbar-style [:i.icon.ion-ios-folder-outline icon-style] "Open"]
    -1
    button-style
    (delay true)
    #(put! out [:open-root-directory])]

   [button
    [:span toolbar-style [:i.icon.ion-ios-compose-outline icon-style] "New"]
    -1
    button-style
    (delay true)
    #(put! out [:new])]

   [button
    [:span toolbar-style [:i.icon.ion-ios-trash-outline icon-style] "Delete"]
    -1
    button-style
    (reagent/cursor opened [:path])
    #(put! out [:delete])]

   [button
    [:span toolbar-style [:i.icon.ion-ios-download-outline icon-style] "Save"]
    -1
    button-style
    (reagent/cursor opened [:dirty?])
    #(put! out [:save])]

   [button
    [:span toolbar-style
     [:span {:style {:margin-right "5px"}} "(...)"]
     "Eval selection"]
    -1
    button-style
    (reagent/cursor opened [:cursor-selection])
    #(put! out [:evaluate-form nil])]

   [button
    [:span toolbar-style [:i.icon.ion-ios-arrow-thin-down icon-style] "Eval file"]
    -1
    button-style
    opened
    #(put! out [:evaluate-script nil])]

   [button
    [:span toolbar-style [:i.icon.ion-ios-download-outline icon-style] "AOT compile"]
    -1
    button-style
    (delay true)
    #(put! out [:aot-compile nil])]])
