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

(defn delete-file-dialog [state-cur channel]
  (go
    (swap! state-cur assoc-in [:delete-file :caption]
           "Are you sure you would like to delete this file?")
    (loop []
      (case (<! channel)
        :yes (let [{:keys [exception]} (<! (clr/async-eval 'core.fs/delete (get-in @state-cur [:opened-file :path])))]
               (if exception
                 (do (swap! state-cur assoc-in [:delete-file :exception] exception)
                     (recur))
                 (swap! state-cur dissoc :opened-file)))
        :no nil
        (recur)))
    (swap! state-cur dissoc :delete-file)
    (<! (load-folder state-cur (root-path @state-cur) channel))))

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

(defn cannot-save-file
  "Displays exception, returns false."
  [state-cur message channel]
  (go
    (swap! state-cur update-in [:save-file] merge {:caption "Cannot save file"
                                                   :exception message})
    (loop []
      (case (<! channel)
        :ok nil
        (recur)))
    (swap! state-cur dissoc :save-file)
    false))

(defn save
  "Returns true if success, false if failed or canceled."
  [state-cur channel]
  (go
    (if (get-in @state-cur [:opened-file :path])
      (let [{:keys [status result]}
            (<! (clr/async-eval
                  'core.fs/save
                  (get-in @state-cur [:opened-file :path])
                  (get-in @state-cur [:opened-file :text])))]
        (case status
          :success (do
                     (swap! state-cur update-in [:opened-file] merge (merge {:dirty? false} result))
                     (<! (load-folder state-cur (root-path @state-cur) channel))
                     true)
          :exception (<! (cannot-save-file state-cur result channel))
          false))
      (let [{:keys [status result]}
            (<! (clr/winforms-async-eval 'core.fs/save-as (root-path @state-cur)))]
        (case status
          :success (do
                     (swap! state-cur update-in [:opened-file] merge result)
                     (save state-cur channel))
          :cancel false
          :exception (<! (cannot-save-file state-cur result channel))
          false)))))

(defn close-file?
  "Returns true if file was closed, false if not."
  [state-cur channel]
  (go
    (if (or
          (get-in @state-cur [:opened-file :dirty?])
          (let [path (get-in @state-cur [:opened-file :path])]
            (when path (not (<! (clr/async-eval 'core.fs/exists path))))))
      (do
        (swap! state-cur assoc-in [:close-file :caption] "Would you like to save this file before closing it?")
        (let [result (loop []
                       (case (<! channel)
                         :yes (if (<! (save state-cur channel))
                                true
                                (recur))
                         :no true
                         :cancel false
                         (recur)))]
          (swap! state-cur dissoc :close-file)
          result))
      true)))

(defn open-folder-browser-dialog [state-cur channel]
  (go
    (when (<! (close-file? state-cur channel))
      (let [{:keys [path cancel exception]} (<! (clr/winforms-async-eval 'core.fs/folder-browser-dialog))]
        (if exception
          (do
            (swap! state-cur assoc-in [:open-root-directory :caption] "Cannot open directory")
            (loop []
              (case (<! channel)
                :ok nil
                (recur)))
            (swap! state-cur dissoc :open-root-directory))
          (if path
            (<! (load-folder state-cur path channel))))))))

(defn next-opened-id [state-cur]
  (:opened-id-count (swap! state-cur update-in [:opened-id-count] inc)))

(defn new-file-dialog [state-cur channel]
  (go
    (when (<! (close-file? state-cur channel))
      (swap! state-cur assoc :opened-file {:id (next-opened-id state-cur)
                                           :name "untitled"
                                           :text ""
                                           :dirty? true}))))

(defn do-open-file [state-cur channel path]
  (go
    (let [{:keys [name text last-write-time exception]} (<! (clr/async-eval 'core.fs/read-all-text path))]
      (if exception
        (do
          (swap! state-cur assoc-in [:open-file :caption] "Cannot open file")
          (loop []
            (case (<! channel)
              :ok nil
              (recur)))
          (swap! state-cur dissoc :open-file))
        (when (and path text)
          (swap! state-cur assoc :opened-file {:id (next-opened-id state-cur)
                                               :path path
                                               :name name
                                               :last-write-time last-write-time
                                               :text text}))))
    (<! (load-folder state-cur (root-path @state-cur) channel))))

(defn open-file [state-cur channel path]
  (go
    (when (<! (close-file? state-cur channel))
      (if (= (get-in @state-cur [:opened-file :path]) path)
        (swap! state-cur dissoc :opened-file)
        (<! (do-open-file state-cur channel path))))))

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
                       (<! (do-open-file state-cur channel path)))
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
          :save (<! (save state-cur channel))
          :reload-file (<! (reload-file state-cur channel))
          :evaluate-form (swap! state-cur evaluate-form)
          :evaluate-script (swap! state-cur evaluate-script)
          :aot-compile (<! (aot-compile state-cur channel))
          :before-change (swap! state-cur data/shift-results arg)
          :change (swap! state-cur data/update-text arg)
          :cursor-selection (swap! state-cur data/update-cursor-selection arg)
          :default)))))
