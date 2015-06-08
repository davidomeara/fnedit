(ns ui.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [ui.debug :as debug]))

(defn stop-event
  ([e]
   (.stopPropagation e)
   (.preventDefault e))
  ([e f]
   (stop-event e)
   (f e)
   nil))

(def modifier-keys
  #{:altKey
    :ctrlKey
    :metaKey
    :shiftKey})

(defn modifier-set
  "Takes an event, returns the set of true modifier keys."
  [e]
  (->> modifier-keys
    (map (fn [k] [k (aget e (name k))]))
    (into {})
    (filter val)
    (map key)
    (into #{})))

(defn key-combination
  "Normalizes "
  [e]
  {:key-code (.-keyCode e)
   :modifiers (modifier-set e)})

(defn key-command [e]
  (case (key-combination e)
    ;Ctrl-O
    {:key-code 79 :modifiers #{:ctrlKey}} :open-root-directory

    ;Ctrl-N
    {:key-code 78 :modifiers #{:ctrlKey}} :new

    ;Ctrl-S
    {:key-code 83 :modifiers #{:ctrlKey}} :save

    ;Ctrl-Enter
    {:key-code 13 :modifiers #{:ctrlKey}} :evaluate-form

    ;Ctrl-Shift-Enter
    {:key-code 13 :modifiers #{:ctrlKey :shiftKey}} :evaluate-script

    ;not a command, pass through
    nil))

;For keyCode to use look here:
;https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/keyCode
(defn make-keydown [channel]
  (fn [e]
    (let [command (key-command e)]
      (when command
        (-> js/document .-activeElement .blur)
        (put! channel [:unhover])
        (stop-event e)
        (put! channel [command])))))
