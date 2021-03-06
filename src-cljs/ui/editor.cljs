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

(ns ui.editor
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as reagent]
            [cljsjs.codemirror]
            [cljsjs.codemirror.mode.clojure]
            [ui.events :as events]
            [ui.debug :as debug]))

(defn line-widget-style [style]
  {:-webkit-user-select "text"
   :overflow-x "auto"
   :overflow-y "hidden"
   :margin "calc(0.5em - 3px)"
   :padding "0.5em"
   :background-color (:background style)
   :border (str "1px solid " (:border-a style))
   :white-space "pre"})

(defn line-widget [style value]
  (let [div (.createElement js/document "div")]
    (->> [:div {:style (line-widget-style style)} value]
      reagent/render-to-static-markup
      (set! (.-innerHTML div)))
    (.-firstChild div)))

(defn clear-line-widgets [cm]
  (.eachLine
    (.-doc cm)
    #(when (.-widgets %)
      (let [widgets (.slice (.-widgets %))]
        (doseq [i (range (alength widgets))]
          (.clear (aget widgets i)))))))

(defn add-line-widget [style cm to value]
  (when (not-empty value)
    (let [doc (.-doc cm)
          to-pos (.posFromIndex doc to)
          line (.-line to-pos)
          line-handle (.getLineHandle doc line)
          div (line-widget style value)]
      (.addLineWidget cm line-handle div))))

(defn widgets-height [cm cursor]
  (reduce
    (fn [a widget]
      (+ a (-> widget .-node .-scrollHeight)))
    0
    (-> cm
      (.lineInfo (.-line cursor))
      .-handle
      .-widgets)))

(defn evaluate-script-results [style cm results]
  (.operation cm
    (fn []
      (clear-line-widgets cm)
      (doseq [[[_ to] v] (sort-by #(first (first %)) results)]
        (add-line-widget style cm to v)))))

(defn get-cm [this]
  (-> this reagent/dom-node .-lastChild .-CodeMirror))

(defn cursor-selection [cm]
  (let [doc (.-doc cm)
        from (.indexFromPos doc (.getCursor doc "from"))
        to (.indexFromPos doc (.getCursor doc "to"))]
    [from to]))

(def placeholder-text
  (str "Code goes here...\n\n"
    "Key commands:\n"
    "  Ctrl-Enter evaluates selected and adjacent forms\n"
    "  Ctrl-Shift-Enter evaluates the entire file"))

(defn count-inserted [c]
  (let [char-count (reduce + (map count (.-text c)))
        newline-count (- (count (.-text c)) 1)]
    (+ char-count
      newline-count)))

(defn make-on-before-change [out]
  (fn [cm c]
    (let [doc (.-doc cm)]
      (put! out
        [:before-change
         [(-> doc (.indexFromPos (.-from c)))
          (count-inserted c)
          (-> doc (.indexFromPos (.-to c)))]]))))

(defn make-editor [channel theme opened-file]
  (let [cached-results (atom nil)
        before-change (make-on-before-change channel)
        change #(put! channel [:change (-> %1 .-doc .getValue)])
        cursor-activity #(put! channel [:cursor-selection (cursor-selection %)])
        on-focus #(put! channel [:focus-editor])
        on-blur #(put! channel [:blur])]

    (reagent/create-class
      {:reagent-render
       (fn []
         [:div {:style {:position "absolute"
                        :width "100%"
                        :height "100%"}}])
       :component-will-update
       (fn [this [_ channel theme opened-file]]
         (let [cm (get-cm this)
               results (:results opened-file)]
           (when (not= results @cached-results)
             (evaluate-script-results theme cm results)
             (reset! cached-results results))
           (.refresh cm)))
       :component-did-update
       (fn [this]
         (let [cm (get-cm this)]
           (.focus cm)))
       :component-did-mount
       (fn [this]
         (let [cm (js/CodeMirror.
                    (reagent/dom-node this)
                    (clj->js {:value (:text opened-file)
                              :tabindex -1
                              :lineNumbers true
                              :styleActiveLine true
                              :lineWrapping false
                              :foldGutter true
                              :matchBrackets true
                              :placeholder placeholder-text
                              :mode "clojure",
                              :theme "default"}))
               style (-> cm .getWrapperElement .-style)]

           (set! (.-fontFamily style) (:code-font-family theme))
           (set! (.-fontSize style) (:code-font-size theme))
           (set! (.-height style) "100%")

           (.on cm "beforeChange" before-change)
           (.on cm "change" change)
           (.on cm "cursorActivity" cursor-activity)
           (.on cm "focus" on-focus)
           (.on cm "blur" on-blur)

           (.focus cm)
           (.refresh cm)))
       :component-did-unmount
       (fn [this]
         (let [cm (get-cm this)]
           (.off cm "beforeChange" before-change)
           (.off cm "change" change)
           (.off cm "cursorActivity" cursor-activity)
           (.off cm "focus" on-focus)
           (.off cm "blur" on-blur)))})))

(defn editor [channel theme opened-file]
  [:div {:style {:display "flex"
                 :flex-grow 1
                 :position "relative"}}
   (when opened-file
     (let [coll [opened-file]]
       (for [x coll]
         ^{:key (:id x)} [make-editor channel theme opened-file])))])