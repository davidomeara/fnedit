(ns ui.core
  (:require [cljs.core.async :refer [chan]]
            [reagent.core :as reagent]
            [ui.clr :as clr]
            [ui.coordination :as coordination]
            [ui.js-util :refer [stop-event]]
            [ui.editor :refer [editor]]
            [ui.hsplitter :refer [hsplitter]]
            [ui.files :refer [files]]
            [ui.toolbar :refer [toolbar]]
            [ui.modal-dialog :as modal-dialog]
            [ui.debug :as debug]))

(def state (reagent/atom nil))

(defn main [debug]
  (when debug
    (enable-console-print!))

  (.addEventListener js/window "dragover" stop-event)
  (.addEventListener js/window "drop" stop-event)

  (let [channel (chan)]

    (coordination/files state channel)

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
         (reagent/cursor state [:folder :path])
         (reagent/cursor state [:opened-file])
         channel]
        [modal-dialog/name-file (reagent/cursor state [:new-file]) channel]
        [modal-dialog/yes-no (reagent/cursor state [:delete-file]) channel]
        [modal-dialog/ok (reagent/cursor state [:ok-dialog]) channel]
        [modal-dialog/ok (reagent/cursor state [:open-folder]) channel]
        [modal-dialog/ok (reagent/cursor state [:open-file]) channel]
        [modal-dialog/ok (reagent/cursor state [:save-file]) channel]
        [modal-dialog/yes-no-cancel (reagent/cursor state [:close-file]) channel]
        [modal-dialog/yes-no (reagent/cursor state [:reloaded-file]) channel]
        [modal-dialog/aot-compile (reagent/cursor state [:aot-compile]) channel]
        [hsplitter
         120
         120
         [files
          (reagent/cursor state [:folder :files])
          (reagent/cursor state [:opened-file])
          channel]
         [editor
          (reagent/cursor state [:folder :files])
          (reagent/cursor state [:opened-file])
          channel]]]
       (when debug
         [(fn []
            [:pre
             {:style {:height "400px"
                      :margin 0
                      :overflow "auto"
                      :border-top "solid 1px #b6b6b7"}}
             (debug/stringify @state)])])]
      (.getElementById js/document "root"))))

(main (clr/sync-eval (str '(core.clojure-clr-wrapper/is-debug))))