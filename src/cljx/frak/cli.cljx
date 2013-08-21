(ns frak.cli
  "Command line interface."
  (:require [clojure.string :as string]
            [guns.cli.optparse :as o]
            [frak]))

;;;; Utilities

#+clj
(defn exit [code]
  (System/exit code))

#+cljs 
(defn exit [code]
  (.exit js/process code))

#+clj
(defn printerr [message]
  (.println *err* message))

#+cljs
(defn printerr [message]
  (.error js/console message))

#+clj
(defn log [message]
  (println message))

#+cljs
(defn log [message]
  (.log js/console message))

;;;; Main 

(def main-flags
  [["-e" "--exact" "Generated pattern requires an exact match"
    :key :exact?]
   ["-c" "--capture" "Generated pattern captures"
    :key :capture?]
   ["-h" "--help" "Display this help message"]])

(defn -main
  "Passes arguments to frak/pattern"
  [& args]
  (try
    (let [[opts words summary] (o/parse args main-flags)]
      (if (or (empty? words) (:help opts))
        (log (str "Usage: frak <flags*> <strings+>\n\nFlags:\n" summary))
        (log (frak/string-pattern words (select-keys opts [:exact? :capture?]))))
      (exit 0))
    (catch #+clj AssertionError #+cljs js/Error e
      (log (#+clj .getMessage #+cljs .-message e))
      (exit 1))))

#+cljs (set! *main-cli-fn* -main)
