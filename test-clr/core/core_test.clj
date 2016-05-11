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

(ns core.core-test
  (:require [clojure.test :refer :all]
            [core.core :as c]))

;;(require '[core.core-test :as t] :reload-all)

(deftest try-read
  (let [string-reader (System.IO.StringReader. "(+ 1 1)")
        line-reader (clojure.lang.LineNumberingTextReader. string-reader)]
    (println (c/try-read (System.Object.) line-reader))))

(deftest lazy-read
  (let [string-reader (System.IO.StringReader. "(+ 1 1) (+ 1 2)")
        line-reader (clojure.lang.LineNumberingTextReader. string-reader)]
    (println (c/lazy-read line-reader))))

(deftest try-eval
  (let [string-writer (System.IO.StringWriter.)]
    (c/try-eval (read-string "(println (+ 1 1))") string-writer)
    (println (.ToString string-writer))))

(deftest lazy-eval
  (let [string-reader (System.IO.StringReader. "(print \"hi\") (print \"dave\")")
        line-reader (clojure.lang.LineNumberingTextReader. string-reader)
        read-results (c/lazy-read line-reader)]
    (println (c/lazy-eval read-results))))

(deftest read-eval
  (println (c/read-eval "(println *ns*)\n\n(println \"dave\")\nasdf\n(+ 1 1)")))

