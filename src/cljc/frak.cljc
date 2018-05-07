(ns frak
  "Generate regular expressions from collections of strings."
  (:require [clojure.string :as string]
            [clojure.set :as set]))

;;;; Trie construction

(defn- trie-node
  "Create a new trie node."
  ([char]
     (trie-node char false))
  ([char terminal?]
     {:char char, :terminal? terminal?, :children #{}}))

(def ^{:private true
       :doc "Root node for new tries."}
  root-node (trie-node nil))

(defn- find-by-char
  "Return a character child of trie node if it exists."
  [node char]
  (-> (:children node)
      (set/index [:char])
      (get {:char char})
      first))

(defn- trie-put
  "Add a sequence of characters to a trie."
  [trie chars]
  (if-not (seq chars)
    trie
    (let [[char & more] chars
          terminal? (not more)]
      (update-in
       trie
       [:children]
       (fn [children]
         (if-let [c (find-by-char trie char)]
           (let [subtrie (trie-put c more)]
             (-> (set/difference children #{c})
                 (conj (update-in subtrie [:terminal?] #(or % terminal?)))))
           (conj children
                 (trie-put (trie-node char terminal?) more))))))))

(defn- build-trie
  "Construct a new character trie from a collection of strings."
  [strs]
  (reduce trie-put root-node strs))

;;;; Pattern rendering

(def ^{:doc "Special characters in various regular expression implementations."}
  metacharacters
  {:default #{\\ \^ \$ \* \+ \? \. \| \( \) \{ \} \[ \]}
   ;; Vimscript "very-magic" mode
   :vim (set (remove #(re-find #"\w" (str %)) (map char (range 0x21 0x7f))))})

(def ^{:private true
       :dynamic true
       :doc "Characters to escape when rendering a regular expression."}
  *escape-chars*
  (:default metacharacters))

(def ^{:private true
       :dynamic true
       :doc "Whether the rendered regex should match only whole words"}
  *whole-words* false)

(defn- escape
  "Escape a character if it is an element of `*escape-chars*`."
  [c]
  (str (when (contains? *escape-chars* c) "\\") c))

(def ^{:private true
       :dynamic true
       :doc "When set to true regular expression groups will capture."}
  *capture* false)

(defn- re-group-start
  "Return the starting delimiter of a regular expression group."
  [capture?]
  (if capture? "(" "(?:"))

(defn- re-group-end
  "Return the ending delimiter of a regular expression group."
  [optional?]
  (if optional? ")?" ")"))

(defn- re-group
  "Convert a collection of strings into a regular expression group. When
   `optional?` is truthy a the group will end with \")?\"."
  ([strs]
     (re-group strs false))
  ([strs optional?]
     (when (seq strs)
       (str
        (re-group-start *capture*)
        (string/join strs)
        (re-group-end optional?)
        (when *whole-words*
          "\\b")))))

(defn- re-or
  "Return a collection of strings joined with a regular expression or
   (`|`) character. Intended to be used with `re-or-group`."
  [strs]
  (string/join "|" strs))

(defn- re-or-group
  "Convert a collection of strings into a regular expression group. When
   `optional?` is truthy a the group will end with \")?\"."
  ([strs]
     (re-group strs false))
  ([strs optional?]
     (when (seq strs)
       (re-group (re-or strs) optional?))))

(defn- re-char-set
  "Convert a collection of characters in to a regular expression
   character set. When `optional?` is truthy a \"?\" is appended."
  ([chars]
     (re-char-set chars false))
  ([chars optional?]
     (when-let [chars (and (seq chars) (map escape chars))]
       (str 
        (if (= 1 (count chars))
          (first chars)
          (str \[ (apply str chars) \]))
        (when optional? "?")))))

(defn- render-trie-strategy [node]
  (let [{:keys [terminal? children]} node]
    (if-not (seq children)
      ::single-char
      (if (= 1 (count children))
        (if terminal?
          ::single-child-terminal
          ::single-child-non-terminal)))))

(defmulti ^:private render-trie
  "Recursively render a trie as a regular expression."
  render-trie-strategy)

(defmethod render-trie ::single-char
  [{:keys [char]}]
  (escape char))

(defmethod render-trie ::single-child-terminal
  [{:keys [char children terminal?]}]
  (let [child (first children)]
    (str
     (escape char)
     (if (and (:terminal? child)
              (not (seq (:children child))))
       (render-trie
        (update-in child [:char] #(str (escape %) "?")))
       (re-group (render-trie child) terminal?)))))

(defmethod render-trie ::single-child-non-terminal
  [{:keys [char children]}]
  (->> (first children)
       (render-trie)
       (str (escape char))))

(defmethod render-trie :default
  [{:keys [char children terminal?]}]
  (let [groups (-> (juxt :terminal? :children)
                   (group-by children)
                   (dissoc nil))
        subpatterns 
        (mapv
         (fn [[_ v]]
           (let [chars (map :char v)
                 char-set (re-char-set chars)
                 subpattern (-> (first v)
                                (dissoc :char)
                                (render-trie))]
             (if (< 1 (count chars))
               (re-group [char-set subpattern])
               (str char-set subpattern))))
         groups)]
    (str (escape char)
         (if (= (first subpatterns) (peek subpatterns))
           (str (peek subpatterns) (when terminal? "?"))
           (re-or-group subpatterns terminal?)))))

;; This is, admittedly, a dirty hack and bat shit crazy but, for now,
;; it gets the job done. Until a more sophisticated data structure or
;; rendering process built this will do for now.
(defn- remove-unecessary-grouping
  [pattern]
  (-> pattern
      ;; Replaces "(?:[xy]z)" and "(?:[xy]z)?" with "[xy]z" and
      ;; "[xy]z?" respectively.
      (string/replace #"\(\?:?(\[[^\]]+\])\)(\?)?"
                      "$1$2")
      ;; Replaces "(?:[ab]cd)" with "[ab]cd".
      (string/replace #"\(\?:?(\[[^\]]+\])([^\|\)]+[^\?]?)\)([^\?])"
                      "$1$2$3")))

(defn- get*
  "Map lookup. In CLJS, also does lookup by string representation of kw."
  [map kw]
  (or (get map kw)
      #?(:cljs (get map (name kw)))))

(def ^:private default-options
  {:capture? false
   :exact? false
   :escape-chars (:default metacharacters)
   :whole-words? false})

(defn string-pattern
  "Construct a regular expression as a string from a collection
   of strings."
  ([strs]
   (string-pattern strs default-options))
  ([strs opts]
   (let [#?@(:cljs [opts (js->clj opts)])
         cs      (or (get* opts :escape-chars) *escape-chars*)
         cs      (if (coll? cs) cs (get* metacharacters cs))
         pattern (binding [*capture*      (get* opts :capture?)
                           *escape-chars* cs
                           *whole-words*  (get* opts :whole-words?)]
                   (-> (build-trie strs)
                       render-trie
                       remove-unecessary-grouping))]
     (if (get* opts :exact?)
       (str "^" pattern "$")
       (if (get* opts :whole-words?)
         (str "\\b" pattern)
         pattern)))))

#?(:cljs (def ^:export stringPattern string-pattern))

(defn ^:export pattern
  "Construct a regular expression from a collection of strings."
  ([strs]
     (pattern strs default-options))
  ([strs opts]
     (re-pattern (string-pattern strs opts))))
