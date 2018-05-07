(ns frak.cli
  "Command line interface."
  (:require [clojure.string :as string]
            #?(:clj  [clojure.tools.cli :refer [parse-opts]]
               :cljs [cljs.tools.cli :refer [parse-opts]])
            [frak]
            [frak.plaform :refer [exit printerr log]]))

;;;; Main 

(def main-flags
  [["-e" "--exact" "Generated pattern requires an exact match"
    :id :exact?]
   ["-c" "--capture" "Generated pattern captures"
    :id :capture?]
   ["-E" "--escape-chars :KEY|'CHARS'"
    "Set of characters to escape in generated pattern"
    :parse-fn #(if (= (first %) \:)
                 (keyword (subs % 1))
                 (set (map first (re-seq #"." %))))]
   ["-h" "--help" "Display this help message"]])

(defn -main
  "Passes arguments to frak/pattern"
  [& args]
  (let [{:keys [options summary errors] words :arguments}
        (parse-opts args main-flags)]
    (when errors
      (printerr (string/join "\n" errors))
      (exit 1))
    (if (or (empty? words) (:help options))
      (log (str "Usage: frak <flags*> <strings+>\n\nFlags:\n" summary))
      (log (frak/string-pattern
            words
            (select-keys options [:exact? :capture? :escape-chars]))))
    (exit 0)))

#?(:cljs (set! *main-cli-fn* -main))
