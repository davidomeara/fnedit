(ns ui.clr
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [cljs.reader :as r]
            [ui.debug :as debug]))

(defn sync-eval [forms]
  (r/read-string (.syncEval js/clr forms)))

(defn winforms-sync-eval [from to forms]
  (r/read-string (.winformsSyncEval js/clr from to forms)))

(def async-channel (chan))

(defn ^:export put-async-channel [x]
  (put! async-channel [(r/read-string x)]))

(defn do-async-eval [form f]
  (go
    (f (str form))
    (first (<! async-channel))))

(defn async-eval [f & args]
  (do-async-eval (cons f args) #(.asyncEval js/clr %)))

(defn winforms-async-eval [f & args]
  (do-async-eval (cons f args) #(.winformsAsyncEval js/clr %)))

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