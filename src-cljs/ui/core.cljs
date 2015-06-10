(ns ui.core
  (:require [cljs.core.async :refer [chan]]
            [reagent.core :as reagent]
            [ui.clr :as clr]
            [ui.events :as events]
            [ui.coordination :as coordination]
            [ui.editor :refer [editor]]
            [ui.hsplitter :refer [hsplitter]]
            [ui.tree-view :refer [tree-view]]
            [ui.toolbar :refer [toolbar]]
            [ui.modal-dialog :as modal-dialog]
            [ui.style :as style]
            [ui.debug :as debug]))

(def initial-state
  {:splitter {:min-left-width 120
              :left-width 120}
   :style style/default-style})

(defn status [style hover focus]
  [:div.unselectable
   {:style {:background-color (:background style)
            :padding "4px"
            :line-height "1.2em"
            :min-height "1.2em"
            :border-top (str "solid 1px " (:border-b style))
            :display "inline-block"}}
   (if-let [h hover] h focus)])

(defn state-viewer [style state]
  [:pre
   {:style {:height "50%"
            :margin 0
            :overflow "auto"
            :border-top (str "solid 1px " (:border-b style))}}
   (debug/stringify state)])

(defn main-component [state channel debug]
  [:div
   {:style {:position "fixed"
            :top 0
            :right 0
            :bottom 0
            :left 0
            :display "flex"
            :flex-direction "column"
            :font-family (:font-family (:style state))
            :font-size (:font-size (:style state))}}
   [:div
    {:style {:display "flex"
             :flex-direction "column"
             :flex-grow 1}}
    [toolbar (:style state) (:opened-file state) channel]
    [modal-dialog/yes-no (:delete-file state) channel]
    [modal-dialog/ok (:open-root-directory state) channel]
    [modal-dialog/ok (:open-file state) channel]
    [modal-dialog/ok (:save-file state) channel]
    [modal-dialog/yes-no-cancel (:close-file state) channel]
    [modal-dialog/yes-no (:reloaded-file state) channel]
    [hsplitter (:splitter state) channel
     [tree-view (:style state) (:root state) (:open-directories state) (:opened-file state) channel]
     [editor (:style state) (:opened-file state) channel]]
    [status (:style state) (:hover state) (:focus state)]]
   (when debug [state-viewer (:style state) state])])

(defn main [debug]
  (when debug
    (enable-console-print!))

  (.addEventListener js/window "contextmenu" events/stop-event true)
  (.addEventListener js/window "dragstart" events/stop-event true)
  (.addEventListener js/window "dragover" events/stop-event true)
  (.addEventListener js/window "drop" events/stop-event true)

  (let [state (reagent/atom initial-state)
        channel (chan)]

    (coordination/process-commands state channel)

    (.addEventListener
      js/window
      "keydown"
      (events/make-keydown channel)
      true)

    (reagent/render
      [(fn [s] [:style (style/css (:style @s) debug)]) state]
      (.getElementById js/document "style"))

    (reagent/render
      [(fn [s] [main-component @s channel debug]) state]
      (.getElementById js/document "root"))))

(main (clr/sync-eval (str '(core.clojure-clr-wrapper/is-debug))))
