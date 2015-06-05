(ns ui.hsplitter
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as reagent]
            [ui.events :as events]
            [ui.debug :as debug]))

(defn set-left-width! [state-cur [client-x]]
  (let [actual-client-x  (- client-x 4)
        min-left-width (get-in @state-cur [:splitter :min-left-width ])
        width (if (< actual-client-x min-left-width)
                min-left-width
                client-x)]
    (swap! state-cur assoc-in [:splitter :left-width] width)
    :drag))

(defn client-x-width [e]
  [(.-clientX e) (-> e .-currentTarget .-clientWidth)])

(defn hsplitter [left right]
  (let [c (chan)
        state-cur (reagent/atom {:splitter {:min-left-width 120
                                            :left-width 120}})
        mouse-down (fn [e] (events/stop-event e #(put! c [:down nil])))
        mouse-move (fn [e] (put! c [:move (client-x-width e)]) nil)
        mouse-up (fn [e] (put! c [:up nil]) nil)]

    (go-loop
     [drag-state nil]
     (recur
      (let [[action arg] (<! c)]
        (case [drag-state action]
          [nil :down] :drag
          [:drag :move] (set-left-width! state-cur arg)
          nil))))

    (reagent/create-class
     {:render
      (fn []
        [:div
         {:on-mouse-move mouse-move
          :style {:flex-grow 1
                  :display "flex"
                  :flex-direction "row"}}
         [:div
          {:style {:width (str (get-in @state-cur [:splitter :left-width]) "px")
                   :min-width (str (get-in @state-cur [:splitter :min-left-width ]) "px")
                   :flex-grow 0
                   :flex-shrink 0
                   :display "flex"
                   :flex-direction "column"}}
          left]
         [:div
          {:on-mouse-down mouse-down
           :style {:cursor "ew-resize"
                   :margin "0 -4px 0 -4px"
                   :width "9px"
                   :z-index "10"
                   :flex-grow 0
                   :flex-shrink 0
                   :display "flex"
                   :flex-direction "column"}}
          [:div
           {:style {:flex-grow 1
                    :width "1px"
                    :margin "0 4px 0 4px"
                    :background-color "#b6b6b7"}}]]
         [:div
          {:style {:flex-grow 1
                   :flex-shrink 0
                   :height "auto"
                   :display "flex"
                   :flex-direction "column"}}
          right]])
      :component-did-mount
      (fn [this]
        (-> js/document .-defaultView (.addEventListener "mouseup" mouse-up false)))
      :component-did-unmount
      (fn []
        (-> js/document .-defaultView (.removeEventListener "mouseup" mouse-up false)))})))
