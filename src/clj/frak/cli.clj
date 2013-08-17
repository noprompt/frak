(ns frak.cli
  (:require [frak]))

(defn -main
  "Passes arguments to frak/pattern"
  [& args]
  (println (str (frak/pattern args)))
  (System/exit 0))
