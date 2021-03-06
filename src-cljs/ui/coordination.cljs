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

(ns ui.coordination
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.set :as set]
            [cljs.core.async :refer [chan <! >! timeout]]
            [ui.data :as data]
            [ui.clr :as clr]
            [ui.debug :as debug]
            [ui.events :as events]
            [ui.utils :as utils]))

(defn root-path [state]
  (->> state
       :root
       first
       ((fn [[k v]] k))
       :path))

(defn- clj-extension? [name]
  (let [n (count name)]
    (if (>= n 4)
      (= (subs name (- n 4) n) ".clj")
      false)))

(defn load-folder [state root-path channel]
  (go
    (swap!
      state
      assoc
      :open-directories
      (<! (clr/async-eval
            'core.fs/remove-deleted-directories
            (:open-directories @state))))
    (swap!
      state
      assoc
      :root
      (<! (clr/async-eval
            'core.fs/root-directory
            root-path
            (:open-directories @state))))))

(defn- assoc-clj-validation-warning [s]
  (assoc-in s [:new-file :validation]
            "The script must have the extension .clj"))

(defn delete-file-dialog [state channel]
  (go
    (swap! state assoc-in [:delete-file :caption]
           "Are you sure you would like to delete this file?")
    (loop []
      (case (-> channel <! first)
        :yes (let [{:keys [exception]} (<! (clr/async-eval 'core.fs/delete (get-in @state [:opened-file :path])))]
               (if exception
                 (do (swap! state assoc-in [:delete-file :exception] exception)
                     (recur))
                 (swap! state dissoc :opened-file)))
        :no nil
        (recur)))
    (swap! state dissoc :delete-file)
    (<! (load-folder state (root-path @state) channel))))

(defn focus [state]
  (if (:opened-file state)
    (assoc-in state [:opened-file :focus] (js/Date.))
    state))

(defn swap-focus! [state]
  (swap! state focus))

(defn evaluate [state to-from-fn]
  (let [opened (:opened-file state)
        [from to] (to-from-fn opened)
        text (:text opened)]
    (->> text
         (clr/winforms-sync-eval (root-path state) from to)
         (data/update-results state)
         focus)))

(defn evaluate-script [state]
  (evaluate
    state
    (fn [opened]
      [0 (count (:text opened))])))

(defn evaluate-form [state]
  (evaluate
    state
    #(if-let [x (:cursor-selection %)] x [-1 -1])))

(defn cannot-save-file
  "Displays exception, returns false."
  [state message channel]
  (go
    (swap! state update-in [:save-file] merge {:caption   "Cannot save file"
                                               :exception message})
    (loop []
      (case (-> channel <! first)
        :ok nil
        (recur)))
    (swap! state dissoc :save-file)
    false))

(declare save)

(defn save-as
  "Returns true if success, false if failed or canceled."
  [state channel]
  (go
    (let [{:keys [status result]}
          (<! (clr/winforms-async-eval
                'core.fs/save-as
                (root-path @state)
                (if-let [n (get-in @state [:opened-file :name])] n "new.clj")))]
      (case status
        :success (do
                   (swap! state update-in [:opened-file] merge result)
                   (save state channel))
        :cancel false
        :exception (<! (cannot-save-file state result channel))
        false))))

(defn save
  "Returns true if success, false if failed or canceled."
  [state channel]
  (go
    (if (get-in @state [:opened-file :path])
      (let [{:keys [status result]}
            (<! (clr/async-eval
                  'core.fs/save
                  (get-in @state [:opened-file :path])
                  (get-in @state [:opened-file :text])))]
        (case status
          :success (do
                     (swap! state update-in [:opened-file] merge (merge {:dirty? false} result))
                     (<! (load-folder state (root-path @state) channel))
                     true)
          :exception (do
                       (<! (cannot-save-file state result channel))
                       (save-as state channel))
          false))
      (save-as state channel))
    (swap-focus! state)))

(defn close-file?
  "Returns true if file was closed, false if not."
  [state channel]
  (go
    (if (or
          (get-in @state [:opened-file :dirty?])
          (let [path (get-in @state [:opened-file :path])]
            (when path (not (<! (clr/async-eval 'core.fs/exists path))))))
      (do
        (swap! state assoc-in [:close-file :caption] "Would you like to save this file before closing it?")
        (let [result (loop []
                       (case (-> channel <! first)
                         :yes (if (<! (save state channel))
                                true
                                (recur))
                         :no true
                         :cancel false
                         (recur)))]
          (swap! state dissoc :close-file)
          result))
      true)))

(defn open-folder-browser-dialog [state channel]
  (go
    (let [{:keys [path cancel exception]} (<! (clr/winforms-async-eval 'core.fs/folder-browser-dialog))]
      (if exception
        (do
          (swap! state assoc-in [:open-root-directory :caption] "Cannot open directory")
          (loop []
            (case (-> channel <! first)
              :ok nil
              (recur)))
          (swap! state dissoc :open-root-directory))
        (when path
          (swap! state assoc :open-directories #{path})
          (<! (load-folder state path channel)))))
    (swap-focus! state)))

(defn next-opened-id [state]
  (:opened-id-count (swap! state update-in [:opened-id-count] inc)))

(defn new-file [state channel]
  (go
    (when (<! (close-file? state channel))
      (swap! state assoc :opened-file {:id     (next-opened-id state)
                                       :name   "untitled"
                                       :text   ""
                                       :dirty? true}))
    (swap-focus! state)))

(defn do-open-file [state channel path]
  (go
    (let [{:keys [name text last-write-time exception]} (<! (clr/async-eval 'core.fs/read-all-text path))]
      (if exception
        (do
          (swap! state assoc-in [:open-file :caption] "Cannot open file")
          (loop []
            (case (-> channel <! first)
              :ok nil
              (recur)))
          (swap! state dissoc :open-file))
        (when (and path text)
          (swap! state assoc :opened-file {:id              (next-opened-id state)
                                           :path            path
                                           :name            name
                                           :last-write-time last-write-time
                                           :text            text}))))
    (<! (load-folder state (root-path @state) channel))))

(defn open-file [state channel path]
  (go
    (when (<! (close-file? state channel))
      (if (= (get-in @state [:opened-file :path]) path)
        (swap! state dissoc :opened-file)
        (<! (do-open-file state channel path))))))

(defn reload-file [state channel]
  (go
    (let [reload-time (<! (clr/async-eval 'core.fs/last-write-time (get-in @state [:opened-file :path])))
          opened-time (get-in @state [:opened-file :last-write-time])]
      (when opened-time
        (if reload-time
          (when (> (.getTime reload-time) (.getTime opened-time))
            (swap! state assoc-in [:reloaded-file :caption] "The file has been changed outside this editor.  Would you like to reload it?")
            (loop []
              (case (-> channel <! first)
                :yes (let [path (get-in @state [:opened-file :path])]
                       (swap! state dissoc :opened-file)
                       (<! (do-open-file state channel path)))
                :no (swap! state assoc-in [:opened-file :dirty?] true)
                (recur)))
            (swap! state dissoc :reloaded-file)
            (swap! state assoc-in [:opened-file :last-write-time] reload-time))
          (swap! state assoc-in [:opened-file :dirty?] true))))
    (<! (load-folder state (root-path @state) channel))))

(defn toggle-open-directory [state channel path]
  (go
    (swap! state update-in [:open-directories] utils/toggle path)
    (<! (load-folder state (root-path @state) channel))))

(defn periodically-send [c v]
  (go
    (while true
      (<! (timeout 2000))
      (>! c v))))

(defn set-left-width! [state client-x]
  (let [actual-client-x (- client-x 4)
        min-left-width (get-in @state [:splitter :min-left-width])
        width (if (< actual-client-x min-left-width)
                min-left-width
                client-x)]
    (swap! state assoc-in [:splitter :left-width] width)
    true))

(defn splitter-down [state channel]
  (swap! state assoc-in [:splitter :dragging] true)
  (go-loop
    []
    (let [[action client-x] (<! channel)]
      (if (case [(get-in @state [:splitter :dragging]) action]
            [true :move] (set-left-width! state client-x)
            [true :up] false
            true)
        (recur)
        (swap! state update-in [:splitter] dissoc :dragging)))))

(defn- path-or-name [opened-file]
  (if-let [path (:path opened-file)]
    path
    (:name opened-file)))

(defn edit-file-status [state]
  (str "Editing " (path-or-name (:opened-file state))))

(defn process-main-commands [state channel]
  (go
    (while true
      (let [[cmd arg] (<! channel)]
        (case cmd
          ; toolbar
          :open-root-directory (<! (open-folder-browser-dialog state channel))
          :new (<! (new-file state channel))
          :delete (<! (delete-file-dialog state channel))
          :save (<! (save state channel))
          :evaluate-form (swap! state evaluate-form)
          :evaluate-script (swap! state evaluate-script)

          ; tree
          :toggle-open-directory (<! (toggle-open-directory state channel arg))
          :open-file (<! (open-file state channel arg))

          ; behind the scenes
          :reload-file (<! (reload-file state channel))

          ; file
          :before-change (swap! state data/shift-results arg)
          :change (swap! state data/update-text arg)
          :cursor-selection (swap! state data/update-cursor-selection arg)

          ; splitter
          :splitter-down (<! (splitter-down state channel))

          :default)))))

(defn process-anytime-commands [state ui-channel main-channel]
  (go
    (while true
      (let [[cmd arg] (<! ui-channel)]
        (swap! state assoc :last-command [cmd arg])
        (case cmd
          ; status
          :hover (swap! state assoc :hover arg)
          :unhover (swap! state dissoc :hover)

          :focus (swap! state assoc :focus arg)
          :focus-editor (swap! state assoc :focus (edit-file-status @state))
          :blur (swap! state dissoc :focus)

          (>! main-channel [cmd arg]))))))

(defn process-commands [state ui-channel]
  (let [main-channel (chan)]
    (process-anytime-commands state ui-channel main-channel)
    (process-main-commands state main-channel)
    (periodically-send ui-channel [:reload-file])))

