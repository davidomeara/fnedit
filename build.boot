(set-env!
  :source-paths #{}
  :resource-paths #{}
  :dependencies '[[org.clojure/clojure "1.7.0-beta2"]
                  [adzerk/boot-cljs "0.0-2814-4" :scope "test"]
                  [org.clojure/clojurescript "0.0-3211" :scope "test"]
                  [org.clojure/core.async "0.1.346.0-17112a-alpha" :scope "test"]
                  [reagent "0.5.0" :scope "test"]
                  [cljsjs/codemirror "5.1.0-0" :scope "test"]
                  [garden "1.2.5"]])

(require
  '[clojure.pprint :as p]
  '[clojure.java.io :as io]
  '[clojure.java.shell :as s]
  '[adzerk.boot-cljs :as boot-cljs])

(defn pp [x]
  (println (with-out-str (p/pprint x))))

(defn pt [x f]
  (f x)
  x)

(def nuget-cache-dir (cache-dir! :nuget/packages :global true))

(deftask include-nuget-cache-dir []
  (with-pre-wrap fileset
    (-> fileset
      (add-resource nuget-cache-dir)
      commit!)))

(deftask nuget
  "Adds nuget package to global nuget repository cache and the current fileset."
  [p package PACKAGE str "The nuget package name."
   v version VERSION str "The nuget package version."]
  (with-pre-wrap fileset
    (println "Nuget is resolving package" package "version" version)
    (pp (s/sh "nuget" "install" package "-Version" version :dir nuget-cache-dir))
    fileset))

(deftask clr-deps []
  (comp
    (nuget :package "Clojure" :version "1.6.0.1")
    (nuget :package "cef.redist.x64" :version "3.2171.2069")
    (nuget :package "CefSharp.Common" :version "39.0.2")
    (nuget :package "CefSharp.WinForms" :version "39.0.2")))

(defn clr-include []
  (comp
    (include-nuget-cache-dir)
    (sift :include #{#".*\.clj$"
                     #"^Clojure.*lib.net40.*dll$"
                     #"^Clojure.*tools.net40.*exe$"
                     #"^cef.redist.x64.3.2171.2069.*CEF.*$"
                     #"^CefSharp\.Common.39.0.2.*CefSharp.x64.*$"
                     #"^CefSharp\.WinForms.39.0.2.*CefSharp.x64.*$"})

    (sift :move {#"^Clojure.*lib.net40." ""
                 #"^Clojure.*tools.net40." ""
                 #"^CefSharp\.Common.*CefSharp.x64." ""
                 #"^CefSharp\.WinForms.*CefSharp.x64." ""})

    ; Order of these matter so they are separated out.
    (sift :move {#"^cef.redist.x64.*CEF.x64." ""})
    (sift :move {#"^cef.redist.x64.*CEF." ""})))

(defn clr-env [compile-path]
  {"CLOJURE_LOAD_PATH" (->> (get-env) :directories (interpose ";") (apply str))
   "CLOJURE_COMPILE_PATH" compile-path
   "SystemDrive" (System/getenv "SystemDrive")})

(defn find-compiler-dir [fileset]
  (-> ["Clojure.Compile.exe"]
    (by-name (output-files fileset))
    first
    :dir))

(deftask clr-compile
  "Compile with the CLR-Clojure compiler."
  [n namespaces NAMESPACES str "Namespaces to compile."]
  (comp
    (with-pre-wrap fileset
      (let [compiler-dir (find-compiler-dir fileset)
            tmp (temp-dir!)]
        (println "Compiling" namespaces)
        (pp (s/sh
              "cmd" "/C"
              (apply str (cons "Clojure.Compile " namespaces))
              :env (clr-env (.getCanonicalPath tmp))
              :dir compiler-dir))
        (-> fileset
          (add-resource tmp)
          commit!)))
    (sift :invert true :include #{#"^Clojure\.Main\.exe$"
                                  #"^Clojure\.Compile\.exe$"})))

(deftask to-gui-subsystem
  "Alters an executable from CLI (CLR-Clojure's default) to GUI subsystem."
  [n names NAMES #{str} "Set of names of executables to be altered."]
  (with-pre-wrap fileset
    (commit!
      (reduce
        (fn [fileset file]
          (let [no-ext (clojure.string/replace (:path file) #".exe$" "")]
            (s/sh "ildasm" (str "/out=" no-ext ".il") (:path file) :dir (:dir file))
            (s/sh "ilasm" "/subsystem=2" (str no-ext ".il") :dir (:dir file))
            (rm fileset (by-name (output-files fileset) #{(str no-ext ".il")
                                                          (str no-ext ".res")}))))
        fileset
        (by-name names (output-files fileset))))))

(deftask start
  "Starts a program."
  [p program PROGRAM str "Program to start."
   a args ARGS (str) "Arguments to pass to the program."]
  (with-post-wrap fileset
    (pp (s/sh
          "cmd"
          "/K"
          (apply str (interpose " " (cons program args)))
          :dir (:target-path (get-env))))
    fileset))

(deftask identity-task [] (with-pre-wrap fileset fileset))

(deftask add-file []
  (with-pre-wrap fileset))

(deftask clr
  "Build for clr."
  [w watch-flag bool "Flag to indicate if watch mode."
   s start-flag bool "Flag to indicate if start at end of build."
   d debug bool "Flag to enable state div, dev tools, console logging, etc."
   f folder bool "Flag to indicate index.html is stored in a separate folder."]
  (set-env!
    :source-paths #{"src-clr"}
    :target-path "target/clr")
  (let [exe "FnEdit.exe"]
    (comp
      (clr-include)
      (if watch-flag (watch) (identity-task))
      (clr-compile :namespaces "core.main")
      (to-gui-subsystem :names #{exe})
      (if start-flag
        (start
          :program exe
          :args (->> {"-d" debug
                      "-f" folder}
                  (filter val)
                  (map key)))
        (identity-task)))))

(deftask cljs
  "Build for clr."
  [w watch-flag bool "Flag to indicate if watch mode."
   d debug bool "Flag to enable cljs source mapping, debugging, etc."]
  (set-env!
    :source-paths #{"src-cljs"}
    :resource-paths #{"web"}
    :target-path "target/cljs")
  (if debug
    (comp
      (if watch-flag (watch) (identity-task))
      (sift :add-jar {'cljsjs/codemirror #"^cljsjs.codemirror.development.codemirror.css$"})
      (sift :move {#"^cljsjs.codemirror.development.codemirror.css$" "cljsjs/codemirror/codemirror.css"})
      (boot-cljs/cljs :optimizations :none :source-map true :pretty-print true))
    (comp
      (if watch-flag (watch) (identity-task))
      (sift :add-jar {'cljsjs/codemirror #"^cljsjs.codemirror.production.codemirror.min.css$"})
      (sift :move {#"^cljsjs.codemirror.production.codemirror.min.css$" "cljsjs/codemirror/codemirror.css"})
      (boot-cljs/cljs :optimizations :advanced)
      (sift :invert true :include #{#"^out.*$"}))))

(deftask input-to-output []
  (with-pre-wrap fileset
    (let [tmp (temp-dir!)]
      (doseq [in (input-files fileset)]
        (io/copy
          (tmpfile in)
          (doto (io/file tmp (tmppath in))
            io/make-parents)))
      (-> fileset
        (add-resource tmp)
        commit!))))

(deftask package []
  (pp (boot (clr)))
  (pp (boot (cljs)))
  (set-env!
    :source-paths #{"target/clr"
                    "target/cljs"}
    :target-path "target/package")
  (comp
    (sift :include #{#"^.*$"})
    (input-to-output)))
