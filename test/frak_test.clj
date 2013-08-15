(ns frak-test
  (:use clojure.test
        frak))

(def trie-put #'frak/trie-put)
(def build-trie #'frak/build-trie)

(deftest trie-test  
  (is (= (-> (trie-put "a")
             (trie-put "b"))
         {\a nil
          \b nil
          :terminals '(\b \a)
          :visitors '(\b \a)}))

  (is (= (-> (trie-put "aaa")
             (trie-put "ab"))
         {\a
          {\a
           {\a nil
            :terminals '(\a)
            :visitors '(\a)}
           \b nil
           :terminals '(\b)
           :visitors '(\b \a)}
          :visitors '(\a \a)}))

  (is (= (-> (trie-put "ab")
             (trie-put "aaa"))
         {\a
          {\a
           {\a nil
            :terminals '(\a)
            :visitors '(\a)}
           \b nil
           :terminals '(\b)
           :visitors '(\a \b)}
          :visitors '(\a\a)})))

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

  (let [pat1 (pattern ["foo" "foot"])
        pat2 (pattern ["foo" "" "foot"])]
    (is (= (str pat1)
           (str pat2))))

  (is (= "ba[trz]"
         (str (pattern ["bat" "bar" "baz"]))))

  (is (= "b(?:i[pt]|at)"
         (str (pattern ["bat" "bip" "bit"])))))
