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
        pat1 (pattern strs1)
        pat2 (pattern strs2)]
    (is (every? #(re-matches pat1 %) strs1))

    (is (every? #(re-matches pat2 %) strs1))

    (is (every? #(re-matches pat1 %) strs2))

    (is (every? #(re-matches pat2 %) strs2))

    (is (not (or (re-matches pat1 "f")
                 (re-matches pat1 "b")
                 (re-matches pat1 "ba")
                 (re-matches pat1 "fo")))))

  (is (= (string-pattern ["foo" "foot"] nil)
         (string-pattern ["foo" "" "foot"] nil)))

  (is (= (re-matches
          #"ba\[[trz]{3}\]"
          (string-pattern ["bat" "bar" "baz"] nil))))

  (is (= "b(?:i[pt]|at)"
         (string-pattern ["bat" "bip" "bit"] nil))))
