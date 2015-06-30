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
              :left-width     120}
   :theme    style/default-theme})

(defn status [theme hover focus]
  [:div
   {:style {:background-color (:background theme)
            :padding          "4px"
            :line-height      "1.2em"
            :min-height       "1.2em"
            :border-top       (str "solid 1px " (:border-b theme))
            :display          "inline-block"}}
   (if-let [h hover] h focus)])

(defn state-viewer [theme state]
  [:pre
   {:style {:height     "50%"
            :margin     0
            :overflow   "auto"
            :border-top (str "solid 1px " (:border-b theme))}}
   (debug/stringify state)])

(defn main-component [channel state debug]
  [:div
   {:style {:-webkit-user-select "none"
            :cursor         "default"
            :position       "fixed"
            :top            0
            :right          0
            :bottom         0
            :left           0
            :display        "flex"
            :flex-direction "column"
            :font-family    (:font-family (:theme state))
            :font-size      (:font-size (:theme state))}}
   [:div
    {:style {:display        "flex"
             :flex-direction "column"
             :flex-grow      1}}
    [toolbar channel (:theme state) (:opened-file state)]
    [modal-dialog/yes-no channel (:theme state) (:delete-file state)]
    [modal-dialog/ok channel (:theme state) (:open-root-directory state)]
    [modal-dialog/ok channel (:theme state) (:open-file state)]
    [modal-dialog/ok channel (:theme state) (:save-file state)]
    [modal-dialog/yes-no-cancel channel (:theme state) (:close-file state)]
    [modal-dialog/yes-no channel (:theme state) (:reloaded-file state)]
    [hsplitter channel (:theme state) (:splitter state)
     [tree-view channel (:theme state) (:root state) (:open-directories state) (:opened-file state)]
     [editor channel (:theme state) (:opened-file state)]]
    [status (:theme state) (:hover state) (:focus state)]]
   (when debug [state-viewer (:theme state) state])])

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
      [(fn [] [:style (style/css (:theme @state) debug)])]
      (.getElementById js/document "theme"))

    (reagent/render
      [(fn [] [main-component channel @state debug])]
      (.getElementById js/document "root"))))

(main (clr/sync-eval (str '(core.clojure-clr-wrapper/is-debug))))
