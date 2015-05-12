(ns ui.data
  (:require [ui.debug :as debug]))

(defn overlap? [from1 to1 from2 to2]
  (and (< from2 to1)
    (< from1 to2)))

(defn overlapped-results [results new-results]
  (for [[[f t] _] results
        [[nf nt] _] new-results]
    (when (overlap? f t nf nt)
      [f t])))

(defn update-results [state new-results]
  (let [opened (:opened-file state)
        results (:results opened)
        results-to-remove (overlapped-results results new-results)
        results-removed (reduce dissoc results results-to-remove)
        results-merged (merge results-removed new-results)]
    (assoc-in state [:opened-file :results] results-merged)))

(defn update-text [state text]
  (-> state
    (assoc-in [:opened-file :text] text)
    (assoc-in [:opened-file :dirty?] true)))

(defn update-cursor-selection [state cursor-selection]
  (assoc-in state [:opened-file :cursor-selection] cursor-selection))

(defn shift-each [results [from inserted to]]
  (let [delta (- to from)
        shift (- inserted delta)]
    (->> results
      (map (fn [[[rf rt] r]]
             (if (overlap? from to rf rt)
               nil
               (if (<= from rf)
                 [[(+ rf shift) (+ rt shift)] r]
                 [[rf rt] r]))))
      (filter #(not (nil? %)))
      (into {}))))

(defn shift-results [state arg]
  (let [results (get-in state [:opened-file :results])]
    (assoc-in state [:opened-file :results] (shift-each results arg))))


