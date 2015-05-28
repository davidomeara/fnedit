(ns ui.coordination
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan <! >! put! timeout alts!]]
            [ui.data :as data]
            [ui.clr :as clr]
            [ui.debug :as debug]
            [clojure.set :as set]))

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

(defn load-folder [state-cur root-path channel]
  (go
    (swap!
      state-cur
      assoc
      :root
      (<! (clr/async-eval 'core.fs/root-directory root-path #{})))))

(defn- assoc-clj-validation-warning [s]
  (assoc-in s [:new-file :validation]
    "The script must have the extension .clj"))

(defn new-file-dialog [state-cur channel]
  (go
    (as-> @state-cur state
      (assoc state :new-file {:caption "New file name:"
                              :file-name "new.clj"
                              :show true})
      (loop [state state]
        (let [state (-> (reset! state-cur state)
                      (update-in [:new-file] dissoc :validation)
                      (update-in [:new-file] dissoc :exception))
              [cmd file-name] (<! channel)]
          (case cmd
            :change (recur
                      (if (clj-extension? file-name)
                        state
                        (assoc-clj-validation-warning state)))
            :ok (if (clj-extension? file-name)
                  (as-> state state
                    (assoc-in state [:new-file :file-name] file-name)
                    (assoc-in state [:new-file :folder-path] (root-path state))
                    (<! (clr/async-eval-in state 'core.fs/combine-folder-path [:new-file]))
                    (<! (clr/async-eval-in state 'core.fs/create [:new-file]))
                    (if (contains? (:new-file state) :path)
                      (as-> state state
                        (<! (load-folder state-cur (root-path state) channel))
                        ; set :new-file to be :opened-file
                        (dissoc state :new-file))
                      (recur state)))
                  (recur (assoc-clj-validation-warning state)))
            :cancel (dissoc state :new-file)
            (recur state))))
      (reset! state-cur state))))

(defn delete-file-dialog [state-cur channel]
  (go
    (as-> @state-cur state
      (assoc-in state [:delete-file :caption]
        "Are you sure you would like to delete this file?")
      (reset! state-cur state)
      (loop [state state]
        (case (<! channel)
          :yes (as-> state state
                 (assoc-in state [:delete-file :path] (get-in state [:opened-file :path]))
                 (<! (clr/async-eval-in state 'core.fs/delete [:delete-file]))
                 (reset! state-cur state)
                 (if (contains? (:delete-file state) :exception)
                   (recur state)
                   (-> state
                     (dissoc :delete-file)
                     (dissoc :opened-file))))
          :no (dissoc state :delete-file)
          (recur state)))
      (<! (load-folder state-cur (root-path state) channel))
      (reset! state-cur state))))

(defn evaluate [state to-from-fn]
  (let [opened (:opened-file state)
        [from to] (to-from-fn opened)
        text (:text opened)]
    (->> text
      (clr/winforms-sync-eval (root-path state) from to)
      (data/update-results state))))

(defn evaluate-script [state]
  (evaluate
    state
    (fn [opened]
      [0 (count (:text opened))])))

(defn evaluate-form [state]
  (evaluate
    state
    #(if-let [x (:cursor-selection %)] x [-1 -1])))

(defn aot-compile [state-cur channel]
  (go
    (as-> @state-cur state
      (assoc state :aot-compile {})
      (reset! state-cur state)
      (loop [state state]
        (let [[cmd arg] (<! channel)]
          (case cmd
            :compile (recur state)
            :close (dissoc state :aot-compile)
            (recur state))))
      (reset! state-cur state))
    #_(println (<! (clr/async-eval 'core.cross-app-domain-compiler/aot-compile
                   {:path (get-in state [:folder :path])
                    :top-ns 'new})))))

(defn save [state-cur state channel]
  (go
    (as-> state state
      (assoc state :save-file (:opened-file state))
      (<! (clr/async-eval-in state 'core.fs/save [:save-file]))
      (if (contains? (:save-file state) :exception)
        (as-> state state
          (assoc-in state [:save-file :caption] "Cannot save file")
          (reset! state-cur state)
          (loop []
            (case (<! channel)
              :ok (dissoc state :save-file)
              (recur))))
        (-> state
          (set/rename-keys {:save-file :opened-file})
          (assoc-in [:opened-file :dirty?] false)))
      (<! (load-folder state-cur (root-path state) channel))
      (reset! state-cur state))))

(defn close-file [state-cur channel on-close-fn]
  (go
    (as-> @state-cur state
      (if (or
            (get-in state [:opened-file :dirty?])
            (let [path (get-in state [:opened-file :path])]
              (when path (not (<! (clr/async-eval 'core.fs/exists path))))))
        (as-> state state
          (assoc-in state [:close-file :caption] "Would you like to save this file before closing it?")
          (reset! state-cur state)
          (loop []
            (case (<! channel)
              :yes (as-> state state
                     (<! (save state-cur state channel))
                     (dissoc state :close-file)
                     (reset! state-cur state)
                     (<! (on-close-fn state)))
              :no (as-> state state
                    (dissoc state :close-file)
                    (reset! state-cur state)
                    (<! (on-close-fn state)))
              :cancel (as-> state state
                        (dissoc state :close-file)
                        (reset! state-cur state))
              (recur))))
        (<! (on-close-fn state))))))

(defn open-folder-browser-dialog [state-cur channel]
  (go
    (<!
      (close-file state-cur channel
        (fn [state]
          (go
            (as-> state state
              (<! (clr/winforms-async-eval-in state 'core.fs/folder-browser-dialog [:open-root-directory]))
              (<! (load-folder state-cur (get-in state [:open-root-directory :path]) channel))
              (reset! state-cur state))))))))

(defn do-open-file [state-cur state channel path]
  (go
    (as-> state state
      (assoc-in state [:open-file :path] path)
      (<! (clr/async-eval-in state 'core.fs/read-all-text [:open-file]))
      (if (contains? (:open-file state) :exception)
        (as-> state state
          (assoc-in state [:open-file :caption] "Cannot open file")
          (reset! state-cur state)
          (loop []
            (case (<! channel)
              :ok (dissoc state :open-file)
              (recur))))
        (if (and
              (get-in state [:open-file :path])
              (get-in state [:open-file :text]))
          (set/rename-keys state {:open-file :opened-file})
          (dissoc state :open-file)))
      (reset! state-cur state)
      (<! (load-folder state-cur (root-path state) channel)))))

(defn open-file [state-cur channel path]
  (go
    (<!
      (close-file state-cur channel
        (fn [state]
          (go
            (as-> state state
              (if (= (get-in state [:opened-file :path]) path)
                (dissoc state :opened-file)
                (<! (do-open-file state-cur state channel path)))
              (reset! state-cur state))))))))

(defn reload-file [state-cur channel]
  (go
    (let [reload-time (<! (clr/async-eval 'core.fs/last-write-time (get-in @state-cur [:opened-file :path])))
          opened-time (get-in @state-cur [:opened-file :last-write-time])]
      (when opened-time
        (if reload-time
          (when (> (.getTime reload-time) (.getTime opened-time))
            (swap! state-cur assoc-in [:reloaded-file :caption] "The file has been changed outside this editor.  Would you like to reload it?")
            (loop []
              (case (<! channel)
                :yes (let [path (get-in @state-cur [:opened-file :path])]
                       (swap! state-cur dissoc :opened-file)
                       (<! (do-open-file state-cur @state-cur channel path)))
                :no (swap! state-cur assoc-in [:opened-file :dirty?] true)
                (recur)))
            (swap! state-cur dissoc :reloaded-file)
            (swap! state-cur assoc-in [:opened-file :last-write-time] reload-time))
          (swap! state-cur assoc-in [:opened-file :dirty?] true))))
    (<! (load-folder state-cur (root-path @state-cur) channel))))

(defn toggle [s v]
  (if (contains? s v)
    (disj s v)
    (clojure.set/union s #{v})))

(defn toggle-open-directory [state-cur path]
  (swap! state-cur update-in [:open-directories] toggle path))

(defn periodically-send [v]
  (let [c (chan)]
    (go
      (while true
        (<! (timeout 1000))
        (>! c v)))
    c))

(defn files [state-cur channel]
  (go
    (while true
      (let [[[cmd arg] _] (alts! [channel (periodically-send [:reload-file nil])])]
        (case cmd
          :open-root-directory (<! (open-folder-browser-dialog state-cur channel))
          :toggle-open-directory (toggle-open-directory state-cur arg)
          :open-file (<! (open-file state-cur channel arg))
          :new (<! (new-file-dialog state-cur channel))
          :delete (<! (delete-file-dialog state-cur channel))
          :save (<! (save state-cur @state-cur channel))
          :reload-file (<! (reload-file state-cur channel))
          :evaluate-form (swap! state-cur evaluate-form)
          :evaluate-script (swap! state-cur evaluate-script)
          :aot-compile (<! (aot-compile state-cur channel))
          :before-change (swap! state-cur data/shift-results arg)
          :change (swap! state-cur data/update-text arg)
          :cursor-selection (swap! state-cur data/update-cursor-selection arg)
          :default)))))
