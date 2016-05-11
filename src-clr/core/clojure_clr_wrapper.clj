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

(ns core.clojure-clr-wrapper
  (:require [core.core :as c]
            [core.aot-compiler]
            [core.fs])
  (:import
   (System.Threading.Tasks
    Task)))

(defn- filter-null-terminator
  "Needed because Chrome stops reading when it encounters a C
   style null terminator in a Javascript string."
  [c]
  (if (= c \u0000)
    \uFFFF
    c))

(defn- filter-UCS2
  "Javascript only supports UCS-2, while .Net is UTF-16.  Filter out
   characters outside the UCS-2 range.  Not entirely sure if this is
   necessary.  I couldn't get clojure to return these chars without
   throwing an error."
  [c]
  (let [limit \uFFFF]
    (if (> c limit)
      limit
      c)))

(defn- filter-chars [s]
  (->> s
    (map filter-UCS2)
    (map filter-null-terminator)
    (apply str)))

(defn- evaluate [dir from to forms]
  (->> (c/read-eval dir from to forms)
    (filter-chars)))

(defn pt [s]
  (println s)
  s)

(defn is-debug [wrapper]
  (:debug? wrapper))

(defn exec [f wrapper]
  (f wrapper))

(defn- simple-eval [wrapper forms]
  (-> forms
      read-string
      (conj 'partial)
      eval
      (exec wrapper)
      pr-str
      filter-chars))

(defn- simple-async-eval [wrapper forms]
  (-> (simple-eval wrapper forms)
      pr-str
      (->> (str "ui.clr.put_async_channel("))
      (str ");")
      (->> (.EvaluateScriptAsync (:browser wrapper)))))

(defprotocol PWrapper
  (AsyncEval [this forms])
  (WinformsAsyncEval [this forms])
  (SyncEval [this forms])
  (WinformsSyncEval [this dir from to forms]))

(defrecord Wrapper [browser debug?]
  PWrapper

  (AsyncEval
   [this forms]
   (Task/Run (gen-delegate Action [] (simple-async-eval this forms)))
   nil)

  (WinformsAsyncEval
   [this forms]
   (.Invoke
    (:browser this)
    (gen-delegate Action [] (simple-async-eval this forms))))

  (SyncEval
   [this forms]
   (simple-eval this forms))

  (WinformsSyncEval
   [this dir from to forms]
   (.Invoke
    (:browser this)
    (sys-func [String] [] (evaluate dir from to forms)))))
