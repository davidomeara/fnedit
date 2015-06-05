(ns ui.modal-dialog
  (:require [cljs.core.async :refer [put!]]
            [reagent.core :as reagent]
            [ui.events :as events]
            [ui.widgets :as widgets]
            [ui.debug :as debug]))

(def z-count (atom 10))

(defn dialog [contents & {:keys [style]}]
  (reagent/create-class
    {:reagent-render
     (fn [contents & {:keys [style]}]
       [:div.fullscreen.transparent
        {:style {:z-index (swap! z-count inc)
                 :display "flex"
                 :flex-direction "column"
                 :justify-content "flex-start"
                 :align-items "center"}}
        [:div.font
         {:style (merge {:color "#222"
                         :border "1px solid #b6b6b7"
                         :background-color "#f5f2f1"
                         :z-index (swap! z-count inc)
                         :margin-top "100px"
                         :padding "20px"
                         :width "400px"
                         :display "flex"
                         :flex-direction "column"} style)}
         contents]])
     :component-did-mount
     (fn [this]
       (-> js/document .-activeElement .blur))}))

(defn choice
  "state {:caption string :exception string} choices {output-key button-type button-name-string}"
  [state choices out]
  (when @state
    [dialog
     [:div.unselectable
      [:span {:style {:margin "2px"
                      :display "flex"
                      :flex-direction "row"}} (:caption @state)]
      [:div {:style {:margin "2px"
                     :min-height "3em"
                     :display "flex"
                     :flex-direction "row"
                     :word-wrap "break-word"}} (:exception @state)]
      [:div {:style {:display "flex"
                     :flex-direction "row-reverse"}}
       (->> choices
         (partition 3)
         (map-indexed
           (fn [i [k t v]]
             ^{:key k} [t v (inc i) (delay true) #(put! out k)]))
         reverse)]]]))

(defn ok
  [state out]
  [choice state [:ok widgets/positive-button "OK"] out])

(defn yes-no
  [state out]
  [choice state [:yes widgets/positive-button "Yes"
                 :no widgets/negative-button "No"] out])

(defn yes-no-cancel
  [state out]
  [choice state [:yes widgets/positive-button "Yes"
                 :no widgets/negative-button "No"
                 :cancel widgets/negative-button "Cancel"] out])