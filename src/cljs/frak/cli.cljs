(ns frak.cli
  (:require [clojure.string :as s]
            [frak]))

(def FLAGS
  [[["-e" "--exact"] "Generated pattern requires an exact match"]
   [["-c" "--capture"] "Generated pattern captures"]
   [["-h" "--help"] "Display this help message"]])

(def flag-re (frak/pattern (mapcat first FLAGS)))

(defn parse-args [args]
  (split-with #(re-matches flag-re %) args))

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
     (format "%s\t%s\t%s\n" message (s/join ", " flags) info))
   "Usage: frak <flags*> <strings+>\n\n"
   FLAGS))

(defn exit [code]
  (.exit js/process code))

(defn help []
  (.error js/console summary)
  (exit 0))

(defn start
  [& args]
  (let [[flags words] (parse-args args)
        opts (flags->opts flags)]
    (if (or (empty? args) (:help? opts))
      (help)
      (let [pat (str (frak/pattern words opts))]
        (println (subs pat 1 (dec (count pat))))))))

(set! *main-cli-fn* start)
