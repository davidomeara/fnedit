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

(assembly-load-with-partial-name "System.Windows.Forms")
(assembly-load-with-partial-name "CefSharp")
(assembly-load-with-partial-name "CefSharp.Core")
(assembly-load-with-partial-name "CefSharp.WinForms")

(ns core.main
  (:gen-class :name "FnEdit")
  (:require [core.clojure-clr-wrapper :refer (->Wrapper)])
  (:import
    (System.IO
      Path)
    (System.Drawing
      Size
      Icon)
    (System.Reflection
      Assembly)
    (System.Threading
      Thread
      ThreadStart
      ApartmentState)
    (System.Windows.Forms
      Application
      Form
      FormStartPosition
      DialogResult
      DockStyle)
    (CefSharp
      Cef
      CefSettings
      LogSeverity)
    (CefSharp.WinForms
      ChromiumWebBrowser)))

(defn as [x t]
  (when (= (type x) t)
    x))

(defn on-init [browser debug?]
  (gen-delegate Action []
    (.RegisterJsObject browser "clr" (->Wrapper browser debug?) true)
    (when debug?
      (.ShowDevTools browser))))

(defn init-delegate [debug?]
  (gen-delegate
    |System.EventHandler`1[CefSharp.IsBrowserInitializedChangedEventArgs]| [sender e]
    (let [browser (as sender ChromiumWebBrowser)]
      (when (and (.IsBrowserInitialized e) (not (= browser nil)))
        (.BeginInvoke browser (on-init browser debug?))))))

(defn exe-dir []
  (-> (Assembly/GetEntryAssembly)
      .get_Location
      Path/GetDirectoryName
      (str "/")))

(defn index-path [folder?]
  (str "file:///"
       (Path/GetFullPath (str (exe-dir) (if folder? "../cljs/" "") "index.html"))))

(defn browser [debug? folder?]
  (doto (ChromiumWebBrowser. (index-path folder?))
    (.set_Dock DockStyle/Fill)
    (.add_IsBrowserInitializedChanged (init-delegate debug?))))

(defn focus-handler [browser]
  (gen-delegate
   |System.EventHandler| [sender e]
   (.SetFocus browser true)))

(defn form [folder? browser]
  (doto (Form.)
    (.set_Icon (Icon. (Path/GetFullPath (str (exe-dir) (if folder? "../cljs/" "") "fn.ico"))))
    (.set_StartPosition FormStartPosition/CenterScreen)
    (.set_MinimumSize (Size. 600 400))
    (.set_Size (Size. 800 600))
    (#(.Add (.get_Controls %) browser))
    (.add_Activated (focus-handler browser))))

(defn sta-thread [f]
  (let [^ThreadStart thread-start (gen-delegate ThreadStart [] (f))]
    (doto (Thread. thread-start)
      (.SetApartmentState ApartmentState/STA)
      .Start
      .Join)))

;https://code.google.com/p/chromium/issues/detail?id=372596
(defn cef-settings []
  (let [settings (CefSettings.)]
    (-> settings
        .get_CefCommandLineArgs
        (.Add "disable-gpu" "1"))
    settings))

(defn -main [& args]
  (sta-thread
   (fn []
     (Cef/Initialize (cef-settings))
     (let [debug? (boolean (some #{"-d"} args))
           folder? (boolean (some #{"-f"} args))
           browser (browser debug? folder?)]
       (.ShowDialog (form folder? browser))
       (.Dispose browser)
       (Cef/Shutdown)
       (shutdown-agents)))))
