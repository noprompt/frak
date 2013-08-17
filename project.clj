(defproject frak "0.1.2"
  :description "Transform collections of strings into regular expressions."
  :url "http://github.com/noprompt/frak"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :profiles {:dev {:dependencies [[criterium "0.4.1"]]}}
  :main frak.cli )
