(ns ui.widgets
  (:require [reagent.core :as reagent]
            [ui.js-util :refer [stop-event]]
            [ui.debug :as debug]))

(defn key-press [f]
  (fn [e]
    (when (= (.-which e) 13)
      (f))
    nil))

;; add submit option
(defn button [title tabindex style enabled-cursor on-click-fn]
  (fn []
    (if @enabled-cursor
      [:a.unselectable.widget-behavior.widget-color.font
       {:tabIndex tabindex
        :on-click #(stop-event % (fn [] (on-click-fn)))
        :on-key-press (key-press on-click-fn)
        :style (merge style {:display "inline-block"
                             :cursor "pointer"})}
       title]
      [:a.unselectable.widget-behavior.disabled-widget-color.font
       {:tabIndex tabindex
        :style (merge style {:display "inline-block"})}
       title])))

(def standard-widget-style
  {:margin "2px"
   :display "inline-block"
   :width "100px"})

(def standard-button-style
  (merge standard-widget-style
    {:text-align "center"}))

(def icon-style {:style {:margin-right "5px"}})

(defn positive-button [caption tabindex enabled-cursor on-click-fn]
  [button
   [:span [:i.icon.ion-checkmark icon-style] caption]
   tabindex
   standard-button-style
   enabled-cursor
   on-click-fn])

(defn negative-button [caption tabindex enabled-cursor on-click-fn]
  [button
   [:span [:i.icon.ion-close icon-style] caption]
   tabindex
   standard-button-style
   enabled-cursor
   on-click-fn])

(defn input [tabindex style value-cursor on-change-fn on-enter-fn]
  (reagent/create-class
    {:component-did-mount #(-> (reagent/dom-node %) .-children (aget 0) .select)
     :render (fn []
               [:div.widget-behavior
                {:style (merge standard-widget-style {:border "1px solid #b6b6b7"
                                                      :background "white"} style)}
                [:input {:tabIndex tabindex
                         :on-change #(on-change-fn (reset! value-cursor (-> % .-target .-value)))
                         :on-key-press (key-press on-enter-fn)
                         :type "text"
                         :value @value-cursor
                         :style {:width "100%"}}]])}))
