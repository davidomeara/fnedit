(ns ui.debug)

(defn dir [x]
  (.dir js/console x))

(defn stringify [x]
  (.stringify js/JSON (clj->js x) nil 2))

(defn ^:export pprint [& xs]
  (println (stringify xs)))

(defn pt [s]
  (pprint s)
  s)