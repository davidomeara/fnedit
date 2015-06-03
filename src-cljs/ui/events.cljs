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

(def dispatched (atom #{}))

(defn- event-handler [event-type channel]
  (fn [e]
    (if (contains? @dispatched e)
      (swap! dispatched disj e)
      (stop-event e #(put! channel [event-type e])))))

(defn capture [event-type channel]
  (.addEventListener
    js/window
    (name event-type)
    (event-handler event-type channel)
    true))

(defn dispatch [element e]
  (swap! dispatched conj e)
  (.dispatchEvent element e))
