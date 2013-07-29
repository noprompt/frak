# frak

frak transforms collections of strings into regular expressions for
matching those strings.

## Usage

```clojure
user> (require 'frak)
nil
user> (frak/pattern ["foo" "bar" "baz" "quux"])
#"(?:ba(?:r|z)|foo|quux)"
```
