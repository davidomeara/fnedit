; Copyright 2016 David O'Meara
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

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

(defn file-div-style [theme opened?]
  {:padding-left "18px"
   :cursor "default"
   :color (if opened? (:active-color theme) (:color theme))
   :background-color (if opened? (:active theme) "transparent")})

(defn file-div [channel theme depth {:keys [path name]} opened-file]
  [:div
   {:style (file-div-style theme (= path (:path opened-file)))}
   [padded-div depth name
    {:on-click #(events/stop-event % (fn [] (put! channel [:open-file path])))}]])

(defn dir-div
  [channel theme depth [{:keys [path name]} {:keys [directories files]}] open-directories opened-file]

  [:div
   {:style {:cursor "default"
            :color (:color theme)
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
       ^{:key (key directory)} [dir-div channel theme (inc depth) directory open-directories opened-file]))

   (let [files (sort-by :name files)]
     (for [file files]
       ^{:key file} [file-div channel theme (inc depth) file opened-file]))])

(defn tree-view [channel theme root open-directories opened-file]
  [:div
   {:style {:flex-grow 1
            :display "flex"
            :flex-direction "column"
            :background-color "#f5f2f1"}}
   [:div
    {:style {:flex-grow 1
             :overflow "auto"
             :white-space "nowrap"
             :margin "0 -1px -1px -1px"
             :background-color "white"}}
    [:div {:style {:display "inline-block"
                   :min-width "100%"}}
     (let [roots (sort-by #(:name (key %)) root)]
       (for [r roots]
         ^{:key (key r)} [dir-div channel theme 1 r open-directories opened-file]))]]])
