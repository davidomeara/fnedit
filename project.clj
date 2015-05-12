(defproject ui "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.cognitect/transit-cljs "0.8.205"]
                 [reagent "0.5.0"]
                 [cljsjs/codemirror "5.1.0-0"]]

  :plugins [[lein-cljsbuild "1.0.5"]]

  :source-paths ["src-clj"]

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src"]
     :compiler {;:externs ["bower_components/codemirror/lib/codemirror.js"
                ;          "bower_components/codemirror/mode/clojure/clojure.js"]
                :closure-warnings {:externs-validation :off
                                   :non-standard-jsdoc :off}
                :output-to "target/main.js"
                :output-dir "target"
                :optimizations :none
                :source-map true
                :pretty-print true}}
    {:id "prod"
     :source-paths ["src"]
     :compiler {;:externs ["bower_components/codemirror/lib/codemirror.js"
                ;          "bower_components/codemirror/mode/clojure/clojure.js"]
                :closure-warnings {:externs-validation :off
                                   :non-standard-jsdoc :off}
                :output-to "../target/web/target/main.js"
                :output-dir "../target/web/target"
                :optimizations :advanced
                :pretty-print false}}]})
