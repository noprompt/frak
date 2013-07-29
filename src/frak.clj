(ns frak
  (:require [clojure.string :as s]))

(comment
  ;; frak generates regular expressions from collections of strings.

  ;; Usage:
  (frak/pattern ["foo" "bar" "baz" "quux"])
  ;; => #"(?:ba(?:z|r)|foo|quux)"
  )

;; Utilities

(defn- prefixes
  "Return a list of all prefixes for a given collection."
  [coll]
  (map-indexed
   (fn [i _]
     (take (inc i) coll))
   coll))

;; Trie construction

(defn- grow [trie [_ & cs :as chars] terminal?]
  (letfn [(visit [inner-trie]
            (let [it (or inner-trie {})
                  lc (last chars)
                  it (if terminal?
                       (vary-meta it update-in [:terminals] conj lc)
                       it)]
              (-> it
                  (vary-meta update-in [:visitors] conj lc)
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

;; Characters to escape when rendering a regular expression. 
(def ^{:private true}
  escape-chars
  #{\\ \^ \$ \* \+ \? \. \| \( \) \{ \} \[ \]})

(def ^{:private true :dynamic true} *capture* false)

(defn- re-group-fmt []
  (let [start (if *capture* "(" "(?:")]
    (str start "%s)")))

(defn- re-group [[s & more :as strs]]
  (if (seq more)
    (format (re-group-fmt) (s/join "|" strs))
    s))

(defn- render-trie [trie]
  (let [{vs :visitors ts :terminals} (meta trie)
        terminal? (set ts)
        ks (->> (keys trie)
                (sort-by (frequencies vs))
                reverse )]
    (re-group
     (for [k ks]
       (let [sk (if (escape-chars k) (str "\\" k) k)]
         (if-let [branch (trie k)]
           (if (terminal? k)
             (->> (render-trie branch)
                  (format (str "%s" (re-group-fmt) "?") sk))
             (->> (render-trie branch)
                  (format "%s%s" sk)))
           sk))))))

(defn pattern
  "Construct a regular expression from a collection of strings."
  ([strs]
     (pattern strs false))
  ([strs capture?]
     {:pre [(every? string? strs)]}
     (binding [*capture* capture?]
       (-> strs
           build-trie
           render-trie
           str
           re-pattern))))
