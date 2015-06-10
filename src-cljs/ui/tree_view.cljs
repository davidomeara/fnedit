(ns ui.tree-view
  (:require [cljs.core.async :refer [put!]]
            [ui.events :as events]
            [ui.debug :as debug]))

(defn padded-div [depth txt attributes]
  [:div
   (merge attributes {:style {:padding-top "4px"
                              :padding-right "5px"
                              :padding-bottom "4px"
                              :padding-left (str (+ 5 (* 8 depth)) "px")}})
   txt])

(defn file-div-style [style opened?]
  {:padding-left "18px"
   :cursor "default"
   :color (if opened? (:active-color style) (:color style))
   :background-color (if opened? (:active style) "transparent")})

(defn file-div [style depth {:keys [path name]} opened-file channel]
  [:div.unselectable
   {:style (file-div-style style (= path (:path opened-file)))}
   [padded-div depth name
    {:on-click #(events/stop-event % (fn [] (put! channel [:open-file path])))}]])

(defn dir-div
  [style depth [{:keys [path name]} {:keys [directories files]}] open-directories opened-file channel]

  [:div.unselectable
   {:style {:cursor "default"
            :color (:color style)
            :background-color "transparent"}}

   [padded-div depth
    [:span
     (let [icon-style {:style {:display "inline-block"
                               :width "14px"
                               :height "14px"}}]
       (if (contains? open-directories path)
         [:i.ion-ios-arrow-down icon-style]
         [:i.ion-ios-arrow-right icon-style]))
     [:span {:style {:padding-left "4px"}} name]]
    {:on-click #(events/stop-event % (fn [] (put! channel [:toggle-open-directory path])))}]

   (let [directory (sort-by #(:name (key %)) directories)]
     (for [directory directories]
       ^{:key (key directory)} [dir-div style (inc depth) directory open-directories opened-file channel]))

   (let [files (sort-by :name files)]
     (for [file files]
       ^{:key file} [file-div style (inc depth) file opened-file channel]))])

(defn tree-view [style root open-directories opened-file channel]
  [:div.unselectable
   {:style {:flex-grow 1
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
     (let [roots (sort-by #(:name (key %)) root)]
       (for [root roots]
         ^{:key (key root)} [dir-div style 1 root open-directories opened-file channel]))]]])
