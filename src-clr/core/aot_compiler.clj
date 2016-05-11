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

(ns core.aot-compiler
  (:require [core.cross-app-domain-compiler])
  (:import (CrossAppDomainCompiler)))

(defn create-delegate [method-name]
  (Delegate/CreateDelegate
   |CrossAppDomainDelegate|
   (.GetMethod (.GetType (CrossAppDomainCompiler.)) method-name)))

(defn create-app-domain []
  (future
    (doto (AppDomain/CreateDomain (.ToString (Guid/NewGuid)))
      ; This forces the Clojure runtime to be pulled in so we
      ; have a "hot" Clojure runtime available waiting for
      ; user to start compilation.
      (.DoCallBack (create-delegate "DummyWork")))))

(def app-domain (atom (create-app-domain)))

(defn compile-on-tmp-app-domain [data]
  (doto @@app-domain
    (.SetData "data" data)
    (.DoCallBack (create-delegate "Compile"))))

(defn aot-compile
  "Compile in a separate app domain which is then unloaded.
   This prevents dlls from locking up when doing AOT compilation."
  [data & _]
  (compile-on-tmp-app-domain data)
  (let [result (.GetData @@app-domain "result")]
    (AppDomain/Unload @@app-domain)
    (reset! app-domain (create-app-domain))
    result))
