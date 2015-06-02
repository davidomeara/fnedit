(ns ui.keys)

(defn tab? [m]
  (= (:which m) 9))