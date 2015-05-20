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
