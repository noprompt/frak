(ns frak
  "Generate regulare expressions from collections of strings."
  (:require [clojure.string :as s]))

;;;; Utilities

(defn- prefixes
  "Return a list of all prefixes for a given collection."
  [coll]
  (map-indexed (fn [i _] (take (inc i) coll)) coll))

(defn- update-meta [obj ks f & args]
  (apply vary-meta obj update-in ks f args))

;;;; Trie construction

(defn- grow [trie [_ & cs :as chars] terminal?]
  (letfn [(visit [inner-trie]
            (let [it (or inner-trie {})
                  lc (last chars)
                  it (if terminal?
                       (update-meta it [:terminals] conj lc) 
                       it)]
              (-> it
                  (update-meta [:visitors] conj lc)
                  (assoc lc (get-in trie chars)))))]
    (if (seq cs)
      (update-in trie (butlast chars) visit)
      (visit trie))))

(defn- trie-put
  ([s] (trie-put {} s))
  ([trie s]
     {:pre [(map? trie) (string? s)]}
     (if-not (seq s)
       trie
       (loop [t trie, ps (prefixes s)]
         (if-let [cs (and (next ps) (first ps))]
           (recur (grow t cs false) (next ps))
           (grow t (first ps) true))))))

(defn- build-trie [strs]
  (reduce trie-put {} strs))

;;;; Pattern rendering

(def ^{:private true
       :doc "Characters to escape when rendering a regular expression."}
  escape-char?
  #{\\ \^ \$ \* \+ \? \. \| \( \) \{ \} \[ \]})

(defn escape [c]
  (if (escape-char? c) (str "\\" c) (str c)))

(def ^{:private true :dynamic true} *capture* false)

(defn- re-group-fmt []
  (str (if *capture* "(" "(?:") "%s)"))

(defn- re-group [[s & more :as strs]]
  (if (seq more)
    (format (re-group-fmt) (s/join "|" strs))
    s))

(defn- render-trie [trie]
  (let [{vs :visitors ts :terminals} (meta trie)
        terminal? (set ts)
        ks (->> (keys trie)
                (sort-by (frequencies vs))
                reverse)]
    (re-group
     (for [k ks]
       (let [sk (escape k)
             fmt (if (terminal? k)
                   (str "%s" (re-group-fmt) "?")
                   "%s%s")]
         (if-let [branch (trie k)]
           (format fmt sk (render-trie branch))
           sk))))))

(defn pattern
  "Construct a regular expression from a collection of strings."
  ([strs]
     (pattern strs false))
  ([strs capture?]
     {:pre [(every? string? strs)]}
     (binding [*capture* capture?]
       (-> strs build-trie render-trie str re-pattern))))
