(ns ui.files
  (:require [cljs.core.async :refer [put!]]
            [ui.js-util :refer [stop-event]]
            [ui.widgets :refer [button]]
            [ui.debug :as debug]))

(defn file-div [file opened channel]
  [:div.font.unselectable
   {:on-click #(stop-event % (fn [] (put! channel [:open-file (:path file)])))
    :style {:padding "4px 5px 6px 5px"
            :cursor "default"
            :color (if (= (:path file) (:path @opened))
                     "white"
                     "#222")
            :background-color (if (= (:path file) (:path @opened))
                                "#2182fb"
                                "transparent")}}
   (:name file)])

(defn files [files opened channel]
  [:div.unselectable
   {:on-context-menu #(stop-event %)
    :style {:flex-grow 1
            :display "flex"
            :flex-direction "column"
            :background-color "#f5f2f1"}}
   [:div.unselectable
    {:style {:flex-grow 1
             :overflow "auto"
             :white-space "nowrap"
             :margin "0 -1px -1px -1px"
             :background-color "white"}}
    [:div {:style {:display "inline-block"
                   :min-width "100%"}}
     (let [file-coll (sort-by :name @files)]
       (for [file file-coll]
         ^{:key (:path file)} [file-div file opened channel]))]]])
