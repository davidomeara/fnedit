(ns ui.modal-dialog
  (:require [cljs.core.async :refer [put!]]
            [reagent.core :as reagent]
            [ui.js-util :refer [stop-event]]
            [ui.widgets :as widgets]
            [ui.debug :as debug]))

(defn dialog [contents]
  [:div.fullscreen.transparent
   {:style {:z-index "11"
            :display "flex"
            :flex-direction "column"
            :justify-content "flex-start"
            :align-items "center"}}
   [:div.font
    {:style {:color "#222"
             :border "1px solid #b6b6b7"
             :background-color "#f5f2f1"
             :z-index "12"
             :margin-top "100px"
             :padding "20px"
             :width "400px"}}
    contents]])

(defn name-file
  "state {:caption string :validation string :exception string :file-name string} :show, out [:cancel input] [:ok input]"
  [state out]
  (fn []
    (let [input-atom (reagent/atom (:file-name @state))]
      (when @state
        [dialog
         [:form.unselectable
          {:on-submit #(stop-event %)}
          [:span {:style {:margin "2px"
                          :display "flex"
                          :flex-direction "row"}} (:caption @state)]
          [:div {:style {:display "flex"
                         :flex-direction "row"}}
           [widgets/input
            0
            {:flex-grow 1}
            input-atom
            #(put! out [:change %])
            #(put! out [:ok @input-atom])]]
          [:div {:style {:margin "2px"
                         :min-height "3em"
                         :display "flex"
                         :flex-direction "row"
                         :word-wrap "break-word"}}
           (str
             (:validation @state)
             (:exception @state))]
          [:div {:style {:display "flex"
                         :flex-direction "row-reverse"}}
           [widgets/negative-button "Cancel" 2 (delay true) #(put! out [:cancel @input-atom])]
           [widgets/positive-button "OK" 1 (delay true) #(put! out [:ok @input-atom])]]]]))))

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
         reverse
         (map-indexed
           (fn [i [k t v]]
             ^{:key k} [t v i (delay true) #(put! out k)])))]]]))

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