(ns ui.utils
  (:require [clojure.set :as set]))

(defn toggle [s v]
  (if (contains? s v)
    (disj s v)
    (set/union s #{v})))
