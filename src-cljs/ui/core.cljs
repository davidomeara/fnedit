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

(def state (reagent/atom nil))

(defn main [debug]
  (when debug
    (enable-console-print!))

  (.addEventListener js/window "contextmenu" events/stop-event true)
  (.addEventListener js/window "dragover" events/stop-event true)
  (.addEventListener js/window "drop" events/stop-event true)

  (let [channel (chan)]

    (coordination/files state channel)

    (.addEventListener
      js/window
      "keydown"
      (events/make-keydown channel)
      true)

    (reagent/render
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
        [toolbar
         (reagent/cursor state [:opened-file])
         channel]
        [modal-dialog/yes-no (reagent/cursor state [:delete-file]) channel]
        [modal-dialog/ok (reagent/cursor state [:ok-dialog]) channel]
        [modal-dialog/ok (reagent/cursor state [:open-root-directory]) channel]
        [modal-dialog/ok (reagent/cursor state [:open-file]) channel]
        [modal-dialog/ok (reagent/cursor state [:save-file]) channel]
        [modal-dialog/yes-no-cancel (reagent/cursor state [:close-file]) channel]
        [modal-dialog/yes-no (reagent/cursor state [:reloaded-file]) channel]
        [hsplitter
         [tree-view
          (reagent/cursor state [:root])
          (reagent/cursor state [:open-directories])
          (reagent/cursor state [:opened-file])
          channel]
         [editor
          (reagent/cursor state [:opened-file])
          channel]]
        [:div.font.unselectable
         {:style {:background-color "#f5f2f1"
                  :padding "2px"
                  :height "1.2em"
                  :border-top "solid 1px #b6b6b7"}}
         (:status @state)]]
       (when debug
         [(fn []
            [:pre
             {:style {:height "600px"
                      :margin 0
                      :overflow "auto"
                      :border-top "solid 1px #b6b6b7"}}
             (debug/stringify @state)])])]
      (.getElementById js/document "root"))))

(main (clr/sync-eval (str '(core.clojure-clr-wrapper/is-debug))))