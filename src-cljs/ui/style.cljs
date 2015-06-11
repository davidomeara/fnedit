(ns ui.style
  (:require [garden.core :as g]))

(def default-style
  {:color            "black"
   :background       "#f7f7f7"   ; light gray
   :border-a         "#dddddd"   ; gray
   :border-b         "#999999"   ; dark gray
   :active-color     "white"
   :active           "#007aff"   ; blue
   :font-family      "Segoe UI, Verdana, Tahoma, Arial"
   :font-size        "12px"
   :code-font-family "Consolas, monospace"
   :code-font-size   "14px"})

(defn css [style debug]
  (g/css
    {:pretty-print? debug}
    [[:.CodeMirror
      {:font-family "Consolas, monospace"
       :font-size "14px"
       :height "100%"}]

     [:input :a {:border 0}]
     [:input:focus :a {:outline 0}]

     [:.unselectable
      {:-webkit-user-select "none"
       :cursor "default"}]]))
