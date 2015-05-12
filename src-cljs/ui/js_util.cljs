(ns ui.js-util)

(defn stop-event
  ([e]
   (.stopPropagation e)
   (.preventDefault e))
  ([e f]
   (stop-event e)
   (f)
   nil))
