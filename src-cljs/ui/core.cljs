(ns ui.core
  (:require [cljs.core.async :refer [chan put!]]
            [reagent.core :as reagent]
            [ui.clr :as clr]
            [ui.events :as events]
            [ui.coordination :as coordination]
            [ui.editor :refer [editor]]
            [ui.hsplitter :refer [hsplitter]]
            [ui.tree-view :refer [tree-view]]
            [ui.toolbar :refer [toolbar]]
            [ui.modal-dialog :as modal-dialog]
            [ui.debug :as debug]))

(def initial-state
  {:splitter {:min-left-width 120
              :left-width 120}})

(defn main-component [state channel debug]
  [:div
   {:style {:position "fixed"
            :top 0
            :right 0
            :bottom 0
            :left 0
            :display "flex"
            :flex-direction "column"}}
   [:div
    {:style {:display "flex"
             :flex-direction "column"
             :flex-grow 1}}
    [toolbar (:opened-file state) channel]
    [modal-dialog/yes-no (:delete-file state) channel]
    [modal-dialog/ok (:open-root-directory state) channel]
    [modal-dialog/ok (:open-file state) channel]
    [modal-dialog/ok (:save-file state) channel]
    [modal-dialog/yes-no-cancel (:close-file state) channel]
    [modal-dialog/yes-no (:reloaded-file state) channel]
    [hsplitter (:splitter state) channel
     [tree-view (:root state) (:open-directories state) (:opened-file state) channel]
     [editor (:opened-file state) channel]]
    [:div.font.unselectable
     {:style {:background-color "#f7f7f7"
              :padding "4px"
              :line-height "1.2em"
              :min-height "1.2em"
              :border-top "solid 1px #999"
              :display "inline-block"}}
     (if-let [h (:hover state)] h (:focus state))]]
   (when debug
     [(fn []
        [:pre
         {:style {:height "50%"
                  :margin 0
                  :overflow "auto"
                  :border-top "solid 1px #999"}}
         (debug/stringify state)])])])

(defn main [debug]
  (when debug
    (enable-console-print!))

  (.addEventListener js/window "contextmenu" events/stop-event true)
  (.addEventListener js/window "dragstart" events/stop-event true)
  (.addEventListener js/window "dragover" events/stop-event true)
  (.addEventListener js/window "drop" events/stop-event true)

  (let [state (reagent/atom initial-state)
        channel (chan)]

    (coordination/files state channel)

    (.addEventListener
      js/window
      "keydown"
      (events/make-keydown channel)
      true)

    (reagent/render
      [(fn [s] [main-component @s channel debug]) state]
      (.getElementById js/document "root"))))

(main (clr/sync-eval (str '(core.clojure-clr-wrapper/is-debug))))