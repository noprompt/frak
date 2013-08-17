(ns frak.cli
  (:require [frak]))

(defn -main
  "Passes arguments to frak/pattern"
  [& args]
  (println (frak/pattern args)))