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

(ns ui.clr
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [cljs.reader :as r]
            [ui.debug :as debug]))

(defn sync-eval [forms]
  (r/read-string (.syncEval js/clr forms)))

(defn winforms-sync-eval [dir from to forms]
  (r/read-string (.winformsSyncEval js/clr dir from to forms)))

(def async-channel (chan))

(defn ^:export put-async-channel [x]
  (put! async-channel [(r/read-string x)]))

(defn do-async-eval [form f]
  (go
    (f (str form))
    (first (<! async-channel))))

(defn args-as-data [args]
  (map #(conj (list %) 'quote) args))

(defn async-eval [f & args]
  (do-async-eval (cons f (args-as-data args)) #(.asyncEval js/clr %)))

(defn winforms-async-eval [f & args]
  (do-async-eval (cons f (args-as-data args)) #(.winformsAsyncEval js/clr %)))

(defn do-async-eval-in [ff m f ks]
  (go (->>
        (get-in m ks)
        (ff f)
        <!
        (assoc-in m ks))))

(defn async-eval-in [m f ks]
  (do-async-eval-in async-eval m f ks))

(defn winforms-async-eval-in [m f ks]
  (do-async-eval-in winforms-async-eval m f ks))