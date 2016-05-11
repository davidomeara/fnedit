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

(ns ui.modal-dialog
  (:require [cljs.core.async :refer [put!]]
            [reagent.core :as reagent]
            [ui.events :as events]
            [ui.widgets :as widgets]
            [ui.debug :as debug]))

(defonce z-count (atom 10))

(defn dialog [_]
  (reagent/create-class
    {:reagent-render
     (fn [contents]
       [:div.fullscreen
        {:style {:position "fixed"
                 :top 0
                 :right 0
                 :bottom 0
                 :left 0
                 :height "auto"
                 :z-index (swap! z-count inc)
                 :background-color "rgba(0, 0, 0, .2)"
                 :display "flex"
                 :flex-direction "column"
                 :justify-content "flex-start"
                 :align-items "center"}}
        [:div
         {:style {:color "black"
                  :background-color "white"
                  :z-index (swap! z-count inc)
                  :margin-top "100px"
                  :padding "20px"
                  :width "400px"
                  :display "flex"
                  :flex-direction "column"}}
         contents]])
     :component-did-mount
     (fn [this]
       (-> js/document .-activeElement .blur))}))

(defn choice
  "state {:caption string :exception string} choices {key button-type button-name-string}"
  [channel theme state choices]
  (if state
    [dialog
     [:div
      [:span {:style {:margin "2px"
                      :display "flex"
                      :flex-direction "row"}} (:caption state)]
      [:div {:style {:margin "2px"
                     :min-height "3em"
                     :display "flex"
                     :flex-direction "row"
                     :word-wrap "break-word"}} (:exception state)]
      [:div {:style {:display "flex"
                     :flex-direction "row-reverse"}}
       (->> choices
         (partition 3)
         (map-indexed
           (fn [tab-index [key button-type caption]]
             ^{:key tab-index} [button-type channel theme key caption {:tab-index (inc tab-index)}]))
         reverse)]]]
    [:span]))

(defn ok [channel theme state]
  [choice channel theme state [:ok widgets/positive-button "OK"]])

(defn yes-no [channel theme state]
  [choice channel theme state [:yes widgets/positive-button "Yes"
                               :no widgets/negative-button "No"]])

(defn yes-no-cancel [channel theme state]
  [choice channel theme state [:yes widgets/positive-button "Yes"
                               :no widgets/negative-button "No"
                               :cancel widgets/negative-button "Cancel"]])
