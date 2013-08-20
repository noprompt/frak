(ns frak.cli
  "Command line interface."
  (:require [clojure.string :as string]
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
  [[["-e" "--exact"] "Generated pattern requires an exact match"]
   [["-c" "--capture"] "Generated pattern captures"]
   [["-h" "--help"] "Display this help message"]])

(defn flag-val [s]
  (condp re-seq s
    #"-(?:e|-exact)" {:exact? true}
    #"-(?:c|-capture)" {:capture? true}
    #"-(?:h|-help)" {:help? true}
    {}))

(defn flags->opts [flags]
  (reduce
   (fn [m flag]
     (merge m (flag-val flag)))
   {}
   flags))

(def summary 
  (reduce
   (fn [message [flags info]]
     (format "%s\t%s\t%s\n" message (string/join ", " flags) info))
   "Usage: frak <flags*> <strings+>\n\n"
   main-flags))

(defn parse-args [args]
  (let [flag? (->> (mapcat first main-flags)
                   (frak/pattern)
                   (partial re-matches))]
    (split-with flag? args)))

(defn -main
  "Passes arguments to frak/pattern"
  [& args]
  (let [[flags words] (parse-args args)
        opts (flags->opts flags)]
    (if (or (empty? args) (:help? opts))
      (log summary)
      (log (frak/string-pattern words opts)))
    (exit 0)))

#+cljs (set! *main-cli-fn* -main)
