(ns ui.editor
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as reagent]
            [cljsjs.codemirror]
            [cljsjs.codemirror.mode.clojure]
            [ui.js-util :refer [stop-event]]
            [ui.debug :as debug]))

(defn line-widget [value]
  (let [div (.createElement js/document "div")
        node (.createTextNode js/document value)]
    (.appendChild div node)
    div))

(defn clear-line-widgets [cm]
  (.eachLine
    (.-doc cm)
    #(when (.-widgets %)
      (let [widgets (.slice (.-widgets %))]
        (doseq [i (range (alength widgets))]
          (.clear (aget widgets i)))))))

(defn add-line-widget [cm to value]
  (when (not-empty value)
    (let [doc (.-doc cm)
          to-pos (.posFromIndex doc to)
          line (.-line to-pos)
          line-handle (.getLineHandle doc line)
          div (line-widget value)]
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

(defn reset-scroll [cm]
  (let [cursor (-> cm .-doc .getCursor)
        coords (.charCoords cm cursor "local")
        height (widgets-height cm cursor)]
    (set! (.-bottom coords) (+ (.-bottom coords) height))
    (.scrollIntoView cm coords 2)))

(defn evaluate-script-results [cm results]
  (.operation cm
    (fn []
      (clear-line-widgets cm)
      (doseq [[[_ to] v] (sort-by #(first (first %)) results)]
        (add-line-widget cm to v))
      (.refresh cm)))
  #_(reset-scroll cm))

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

(defn make-editor [opened channel]
  (let [cached-results (atom nil)
        before-change (make-on-before-change channel)
        change #(put! channel [:change (-> %1 .-doc .getValue)])
        cursor-activity #(put! channel [:cursor-selection (cursor-selection %)])]

    (reagent/create-class
      {:render
       (fn []
         [:div {:style {:flex-grow 1
                        :position "absolute"
                        :width "100%"
                        :height "100%"
                        :display "flex"
                        :flex-direction "column"}}
          [:div.unselectable
           {:on-context-menu #(stop-event %)
            :style {:display "flex"
                    :flex-direction "row"
                    :border-bottom "1px solid #b6b6b7"
                    :background-color "#f5f2f1"}}
           [:div.font.unselectable
            {:style {:flex-grow 0
                     :padding "4px"
                     :color "white"
                     :background-color "#2182fb"}} (:name @opened)]]
          [:div {:style {:display "none"}} (:results @opened)]])
       :component-will-update
       (fn [this]
         (let [cm (get-cm this)
               results (:results @opened)]
           (when (not= results @cached-results)
             (evaluate-script-results cm results)
             (reset! cached-results results))))
       :component-did-mount
       (fn [this]
         (let [cm (js/CodeMirror.
                    (reagent/dom-node this)
                    (clj->js {:value (:text @opened)
                              :tabindex 20
                              :lineNumbers true
                              :styleActiveLine true
                              :lineWrapping false
                              :foldGutter true
                              :matchBrackets true
                              :placeholder placeholder-text
                              :extraKeys {:Ctrl-O #(put! channel [:open-root-directory nil])
                                          :Ctrl-N #(put! channel [:new nil])
                                          :Ctrl-S #(put! channel [:save nil])
                                          :Ctrl-Enter #(put! channel [:evaluate-form nil])
                                          :Shift-Ctrl-Enter #(put! channel [:evaluate-script nil])}
                              :mode "clojure",
                              :theme "default"}))]
           (.on cm "beforeChange" before-change)
           (.on cm "change" change)
           (.on cm "cursorActivity" cursor-activity)))
       :component-did-unmount
       (fn [this]
         (let [cm (get-cm this)]
           (.off cm "beforeChange" before-change)
           (.off cm "change" change)
           (.off cm "cursorActivity" cursor-activity)))})))

(defn editor [opened channel]
  [:div {:style {:flex-grow 1
                 :position "relative"}}
   (when @opened
     (let [coll [@opened]]
       (for [x coll]
         ^{:key (:id x)} [make-editor opened channel])))])