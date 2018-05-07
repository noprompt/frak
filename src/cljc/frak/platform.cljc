(ns frak.platform)

(defn exit [code]
  #?(:clj  (System/exit code)
     :cljs (.exit js/process code)))

(defn printerr [message]
  #?(:clj  (.println *err* message)
     :cljs (.error js/console message)))

(defn log [message]
  #?(:clj (println message)
     :cljs (.log js/console message)))
