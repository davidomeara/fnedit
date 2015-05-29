(ns ui.tree-view
  (:require [cljs.core.async :refer [put!]]
            [ui.js-util :refer [stop-event]]
            [ui.widgets :refer [button]]
            [ui.debug :as debug]))

(defn padded-div [depth txt]
  [:div
   {:style {:padding-top "4px"
            :padding-right "5px"
            :padding-bottom "6px"
            :padding-left (str (+ 5 (* 8 depth)) "px")}}
   txt])

(defn div-style [opened?]
  {:cursor "default"
   :color (if opened? "white" "#222")
   :background-color (if opened? "#2182fb" "transparent")})

(defn file-div [depth {:keys [path name]} opened-file channel]
  [:div.font.unselectable
   {:on-click #(stop-event % (fn [] (put! channel [:open-file path])))
    :style (div-style (= path (:path @opened-file)))}
   [padded-div depth name]])

(defn dir-div
  [depth [{:keys [path name]} {:keys [directories files]}] open-directories opened-file channel]
  [:div.font.unselectable
   {:on-click #(stop-event % (fn [] (put! channel [:toggle-open-directory path])))
    :style (div-style (contains? open-directories path))}

   [padded-div depth
    [:span
     (if (and
           (nil? directories)
           (nil? files))
       [:i.ion-arrow-right-b]
       [:i.ion-arrow-down-b])
     [:span {:style {:padding-left "4px"}} name]]]

   (let [directory (sort-by #(:name (key %)) directories)]
     (for [directory directories]
       ^{:key (key directory)} [dir-div (inc depth) directory open-directories opened-file channel]))

   (let [files (sort-by :name files)]
     (for [file files]
       ^{:key file} [file-div (inc depth) file opened-file channel]))])

(defn tree-view [root open-directories opened-file channel]
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
     (let [roots (sort-by #(:name (key %)) @root)]
       (for [root roots]
         ^{:key (key root)} [dir-div 0 root open-directories opened-file channel]))]]])
