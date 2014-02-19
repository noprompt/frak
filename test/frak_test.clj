(ns frak-test
  (:use clojure.test
        frak))

(def trie-put #'frak/trie-put)
(def build-trie #'frak/build-trie)

(deftest trie-test  
  (is (= (build-trie ["a" "b"]) 
         {:char nil
          :terminal? false
          :children #{{:char \a
                       :terminal? true
                       :children #{}}
                      {:char \b
                       :terminal? true
                       :children #{}}}}))

  (is (= (build-trie ["aaa" "ab"])
         (build-trie ["ab" "aaa"])
         {:char nil
          :terminal? false
          :children #{{:char \a
                       :terminal? false
                       :children #{{:char \a
                                    :terminal? false
                                    :children #{{:char \a
                                                 :terminal? true
                                                 :children #{}}}}
                                   {:char \b
                                    :terminal? true
                                    :children #{}}}}}})))

(deftest pattern-test
  (let [strs1 ["foo" "bar" "baz"]
        strs2 ["baz" "bar" "foo"]
        match1 (partial re-matches (pattern strs1))
        match2 (partial re-matches (pattern strs2))]
    (is (every? match1 strs1))

    (is (every? match2 strs1))

    (is (every? match1 strs2))

    (is (every? match2 strs2))

    (is (not (or (match1 "f")
                 (match1 "b")
                 (match1 "ba")
                 (match1 "fo")))))

  (is (= (string-pattern ["foo" "foot"] nil)
         (string-pattern ["foo" "" "foot"] nil)))

  (is (= (re-matches
          #"ba\[[trz]{3}\]"
          (string-pattern ["bat" "bar" "baz"] nil))))

  (is (= "b(?:i[pt]|at)"
         (string-pattern ["bat" "bip" "bit"] nil)))

  (is (= "foo\\??"
         (string-pattern ["foo" "foo?"])))

  (is (= "\\!\\\"\\#\\%\\&\\'\\,\\-\\/\\:\\;\\<\\=\\>\\@\\`\\~"
         (string-pattern ["!\"#%&',-/:;<=>@`~"] {:escape-chars :vim})))

  (is (= "foo\\★?"
         (string-pattern ["foo" "foo★"] {:escape-chars #{\★}})))

  (are [words] (every? #(re-matches (pattern words) %) words)
    ["achy" "achylia" "achylous" "achymia" "achymous"]
    ["aching" "achingly"]))
