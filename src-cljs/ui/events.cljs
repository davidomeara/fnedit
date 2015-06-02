(ns ui.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [ui.debug :as debug]
            [ui.utils :as utils]))

(defn stop-event
  ([e]
   (.stopPropagation e)
   (.preventDefault e))
  ([e f]
   (stop-event e)
   (f e)
   nil))

(def captured (atom #{}))

(defn captured? [e]
  (contains? (swap! captured utils/toggle e) e))

(defn- event-handler [event-type channel]
  (fn [e]
    (stop-event e)
    (when (captured? e)
      (put! channel [event-type e]))))

(defn capture [event-type channel]
  (.addEventListener
    js/window
    (name event-type)
    (event-handler event-type channel)
    true))

(defn dispatch [element e]
  (debug/dir (.fromCharCode js/String (.-which e)))
  (.dispatchEvent element e))
