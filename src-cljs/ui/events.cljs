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
        (stop-event e)
        (put! channel [command])))))
