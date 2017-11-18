(defproject frak "0.1.8-SNAPSHOT"
  :description "Transform collections of strings into regular expressions."
  :url "http://github.com/noprompt/frak"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :jar-exclusions [#"(?:\.(?:cljx|sw[onp])|cli\.cljs?)"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "0.0-2156"]
                 [org.clojure/tools.cli "0.3.1"]]
  :plugins [[lein-cljsbuild "1.0.2"]]
  :source-paths ["src/cljc"] 
  :profiles {:dev {:dependencies [[criterium "0.4.1"]]}} 
  :cljsbuild {:builds [{:id           "browser"
                        :source-paths ["target/classes"]
                        :compiler     {:output-to     "target/js/frak.min.js"
                                       :optimizations :advanced
                                       :pretty-print  false}}
                       {:id           "node"
                        :source-paths ["target/classes"]
                        :compiler     {:output-to     "bin/frak"
                                       :externs       ["resources/externs/process.js"]
                                       :optimizations :advanced
                                       :pretty-print  false
                                       :target        :nodejs}}]}
  :main frak.cli
  :repl-options {:init-ns frak})
