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

(def ^{:private true
       :doc "Characters to escape when rendering a regular expression."}
  escape-chars
  #{\\ \^ \$ \* \+ \? \. \| \( \) \{ \} \[ \]})

(defn- escape
  "Escape a character if it is an element of `escape-chars`."
  [c]
  (str (when (escape-chars c) "\\") c))

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
        (string/join "|" strs)
        (re-group-end optional?)))))

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
          (format "[%s]" (apply str chars)))
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
  [{:keys [char children]}]
  (let [child (first children)]
    (str
     (escape char)
     (if (and (:terminal? child)
              (not (seq (:children child))))
       (render-trie
        (update-in child [:char] #(str % "?")) )
       (str (re-group-start *capture*)
            (render-trie child)
            (re-group-end true))))))

(defmethod render-trie ::single-child-non-terminal
  [{:keys [char children]}]
  (str (escape char) (render-trie (first children))))

(defmethod render-trie :default
  [{:keys [char children terminal?]}]
  (let [groups (-> (juxt :terminal? :children)
                   (group-by children)
                   (dissoc nil))
        grouped 
        (mapv
         (fn [[_ v]]
           (str (re-char-set (map :char v))
                (-> (first v)
                    (dissoc :char)
                    (render-trie)
                    ;; Replace instances of "(?:[abc])" with "[abc]".
                    ;; This is such an ugly hack.
                    (string/replace #"\(\?:?(\[[^\]]+\])\)" "$1"))))
         groups)]
    (str (escape char)
         (if (= (first grouped) (peek grouped))
           (str (peek grouped) (when terminal? "?"))
           (re-group grouped terminal?)))))

(defn string-pattern
  "Construct a regular expression as a string from a collection
   of strings."
  [strs opts]
  (let [pattern (binding [*capture* (:capture? opts)]
                  (-> strs build-trie render-trie))]
    (if (:exact? opts)
      (str "^" pattern "$")
      pattern)))

(defn ^:export pattern
  "Construct a regular expression from a collection of strings."
  ([strs]
     (pattern strs {:capture? false, :exact? false}))
  ([strs opts]
     (re-pattern (string-pattern strs opts))))
