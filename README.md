# frak

frak transforms collections of strings into regular expressions for
matching those strings. The primary goal of this library is to
generate regular expressions from a known set of inputs which avoid
backtracking as much as possible. It is available as a [command line
utility](#command-line-usage) and for the [browser](#browser-usage)
as a JavaScript library.

## "Installation"

Add frak as a dependency to your `project.clj` file.

[![Clojars Project](https://img.shields.io/clojars/v/frak.svg)](https://clojars.org/frak)


## Clojure(Script) usage

```clojure
user> (require 'frak)
nil
user> (frak/pattern ["foo" "bar" "baz" "quux"])
#"(?:ba[rz]|foo|quux)"
user> (frak/pattern ["Clojure" "Clojars" "ClojureScript"])
#"Cloj(?:ure(?:Script)?|ars)"
user> (frak/pattern ["skill" "skills" "skull" "skulls"])
#"sk(?:[ui]lls?)"
```

## Options
Frak's `pattern` function can accept an options map as its second argument, the available options are:

* `:capture?` - boolean (default false), whether rendered regex should create capture groups for each match
* `:escape-chars` - vector (default see [`frak/metacharacters`](https://github.com/noprompt/frak/blob/3c22d9b33646ca4bc681a5746ac0d11fd06e579b/src/cljx/frak.cljx#L54)), characters to escape when rendering a regular expression.
* `:exact?` - boolean (default false), whether the rendered regex should only produces matches when the entire input string matches.
* `:whole-words?` - boolean (default false), whether the rendered regex should match only whole words (word boundary at both ends of the match) in the input string.

## Command line usage

frak can be used from the command line with either Leiningen or NodeJS.

### With Leiningen

Use the `lein run` command:

```shell
$ lein run -e foo bar baz quux
^(?:ba[rz]|foo|quux)$
```

### With NodeJS

Compile the NodeJS version

```shell
$ lein do cljx once, cljsbuild once node
$ chmod +x bin/frak
$ bin/frak -e foo bar baz quux
^(?:ba[rz]|foo|quux)$
```

## Browser usage

To use frak as a standalone library in the browser with JavaScript
compile the browser version:

```shell
$ lein do cljx once, cljsbuild once browser
$ mv ./target/js/frak.min.js <destination>
```

Try it using this HTML:

```html
<!DOCTYPE html>
<html>
<head>
</head>
<body>
  <pre>Input: <span id="input"></span></pre>
  <pre>Output: <span id="output"></span></pre>
  <script src="http://code.jquery.com/jquery-2.0.3.min.js"></script>
  <script src="frak.min.js"></script>
  <script>
    var strings = ["foo", "bar", "baz", "quux"];
    // It's a good idea to use the `"exact?"` option.
    var pattern = frak.pattern(strings, {"exact?": true})
    jQuery("#input").text(strings.join(" "));
    jQuery("#output").text(pattern);
  </script>
</body>
</html>
```

For even more fun try it with [AngularJS](http://angularjs.org/)!

## How?

A frak pattern is constructed from a trie of characters and a
renderer which processes it. As characters are added to the trie, data
such as such as which characters are terminal are stored in it's
branches.

During the rendering process frak analyzes each branch and attempts to
emit the most concise regular expression possible. Additional post
operations are applied after rendering to improve the expression where
possible.

## Why?

[Here's](https://github.com/guns/vim-clojure-static/blob/249328ee659190babe2b14cd119f972b21b80538/syntax/clojure.vim#L91-L92)
why. Also because.

## And now for something completely different

Let's build a regular expression for matching any word in
`/usr/share/dict/words`.

```clojure
user> (require '[clojure.java.io :as io])
nil
user> (def words
           (-> (io/file "/usr/share/dict/words")
               io/reader
               line-seq))
#'user/words
user> (def word-re (frak/pattern words))
#'user/word-re
user> (every? #(re-matches word-re %) words)
true
```

The last two operations will take a moment since there are over
235,000 words to consider.

You can view the full expression
[here](https://gist.github.com/noprompt/6106573/raw/fcb683834bb2e171618ca91bf0b234014b5b957d/word-re.clj)
(it's approximately `1.5M`!).

## Benchmarks

```clojure
(use 'criterium.core)

(def words
  (-> (io/file "/usr/share/dict/words")
      io/reader
      line-seq))

(defn naive-pattern
  "Create a naive regular expression pattern for matching every string
   in strs."
  [strs]
  (->> strs
       (clojure.string/join "|")
       (format "(?:%s)")
       re-pattern))

;; Shuffle 10000 words and build a naive and frak pattern from them.
(def ws (shuffle (take 10000 words)))

(def n-pat (naive-pattern ws))
(def f-pat (frak/pattern ws))

;; Verify the naive pattern matches everything it was constructed from.
(every? #(re-matches n-pat %) ws)
;; => true

;; Shuffle the words again since the naive pattern is built in the
;; same order as it's inputs.
(def ws' (shuffle ws))

;;;; Benchmarks

;; Naive pattern

(bench (doseq [w ws'] (re-matches n-pat w)))
;;             Execution time mean : 1.499489 sec
;;    Execution time std-deviation : 181.365166 ms
;;   Execution time lower quantile : 1.337817 sec ( 2.5%)
;;   Execution time upper quantile : 1.828733 sec (97.5%)

;; frak pattern

(bench (doseq [w ws'] (re-matches f-pat w)))
;;             Execution time mean : 155.515855 ms
;;    Execution time std-deviation : 5.663346 ms
;;   Execution time lower quantile : 148.168855 ms ( 2.5%)
;;   Execution time upper quantile : 164.164294 ms (97.5%)
```
