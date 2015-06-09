(ns ui.style
  (:require [garden.core :as g]))

(defn css [debug]
  (g/css
    {:pretty-print? debug}
    [[:body
      {:margin 0}]

     ; Override some default styles
     [:.CodeMirror
      {:font-family "Consolas, monospace"
       :font-size "14px"
       :height "100%"}]

     [:.font
      {:font-family "Segoe UI, Verdana, Tahoma, Arial"
       :font-size "12px"}]

     [:input :a {:border 0}]
     [:input:focus :a {:outline 0}]

     [:.unselectable
      {:-webkit-user-select "none"
       :cursor "default"}]

     [:.button
      {:padding "2px 5px 4px 5px"
       :color "black"
       :border "1px solid transparent"}]
     [:.button:hover :.button:focus
      {:color "#007aff"
       :border "1px solid #007aff"}]
     [:.button:active :.button:active
      {:color "white"
       :background-color "#007aff"
       :border "1px solid #007aff"}]]))
