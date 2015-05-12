(ns ui.macros)

(defmacro to-str [forms]
  (str forms))

;;   (:require-macros [cljs.core.async.macros :refer [go]]
;;                    [ui.macros :refer [to-str clr-go]])
;;  Pretty much useless, but keep as an example

(defmacro <? [expr]
  `(ui.util/throw-err (cljs.core.async/<! ~expr)))
