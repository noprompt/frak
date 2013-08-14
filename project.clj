(defproject frak "0.1.2"
  :description "Transform collections of strings into regular expressions."
  :url "http://github.com/noprompt/frak"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :plugins [[lein-cljsbuild "0.3.2"]]
  :source-paths ["src/clj"]
  :profiles {:dev {:dependencies [[criterium "0.4.1"]]}}
  :cljsbuild {:crossovers [frak]
              :crossover-path "crossovers"
              :crossover-jar true
              :builds [{:id "dev"
                        :source-paths ["src/clj" "src/cljs"]
                        :compiler {:output-to "bin/frak.dev.js"
                                   :optimizations :simple
                                   :pretty-print true
                                   :target :nodejs}}
                       
                       {:id "prod"
                        :source-paths ["src/clj" "src/cljs"]
                        :compiler {:output-to "bin/frak.prod.js"
                                   :optimizations :simple
                                   :pretty-print false
                                   :target :nodejs}}]}
  :main frak.cli)
