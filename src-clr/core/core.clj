(ns core.core
  (:require [core.cross-app-domain-compiler :as c])
  (:import (CrossAppDomainCompiler)))

(defn fuller-exception [^System.Exception ex]
  (str (.ToString ex)
       (if (nil? (.InnerException ex))
         ""
         (.ToString (.InnerException ex)))))

(defn result-make [from to success? item]
  {:from from
   :to to
   :success? success?
   :item item})

(defn result-make-empty []
  (result-make 0 0 true ""))

;; Checkout LispReader
(defn whitespace? [ch]
  (or (and (not= ch -1) (Char/IsWhiteSpace ch)) (= ch \,)))

(defn try-read [^System.Object eof
                ^clojure.lang.LineNumberingTextReader reader]

  (loop [ch (.Read reader)]
    (if (whitespace? ch)
      (recur (.Read reader))
      (when (not= ch -1)
        (.Unread reader ch))))

  (let [from (.Index reader)]
    (try
      (let [item (read reader false eof false)]
        (result-make from
                     (.Index reader)
                     true
                     item))
      (catch System.Exception ex
        (result-make from
                     (.Index reader)
                     false
                     (fuller-exception ex))))))

(defn lazy-read [^clojure.lang.LineNumberingTextReader reader]
  (let [eof (System.Object.)]
    (take-while #(not= (:item %) eof) (repeatedly #(try-read eof reader)))))

(defn try-eval [item ^System.IO.StringWriter string-writer]
  (binding [*out* string-writer
            *err* string-writer]
    (try
      (eval item)
      true
      (catch System.Exception ex
        (.Write string-writer (fuller-exception ex))
        false))))

(defn eval-result [read-result]
  (let [string-writer (System.IO.StringWriter.)]
    (if (:success? read-result)
      (-> read-result
          (assoc :success? (try-eval (:item read-result) string-writer))
          (assoc :item (.ToString string-writer)))
      read-result)))

(defn overlap? [x1 x2 {y1 :from y2 :to}]
  (and (<= x1 y2) (<= y1 x2)))

(defn lazy-eval [from to read-results]
    (for [read-result read-results]
      (if (overlap? from to read-result)
        (eval-result read-result)
        (assoc read-result :item ""))))

(defn limit-exceptions [n s]
  (->> s
       (map (fn [r] [(if (:success? r) 0 1) r]))
       (reductions (fn [[exs rs] [ex r]] [(+ exs ex) (conj rs r)]) [0 (result-make-empty)])
       (take-while (fn [[exs rs]] (<= exs n)))
       (map (fn [[exs rs]] rs))))

(defn results-to-map [s]
  (into {} (map (fn [x] [[(:from x) (:to x)] (:item x)]) s)))

(defn using-line-numbering-reader [string f]
  (let [string-reader (System.IO.StringReader. string)
        line-reader (clojure.lang.LineNumberingTextReader. string-reader)
        result (doall (f line-reader))]
    (.Dispose line-reader)
    result))

(def current-ns (atom (create-ns 'user)))

(defn read-eval [dir from to forms]
  (binding [*ns* @current-ns]
    (c/add-clojure-load-paths dir)
    (let [result (pr-str
                  (using-line-numbering-reader
                   forms
                   (fn [line-reader]
                     (->> line-reader
                          lazy-read
                          (lazy-eval from to)
                          (limit-exceptions 10)
                          (filter #(not= (:item %) ""))
                          results-to-map))))]
      (reset! current-ns *ns*)
      result)))
