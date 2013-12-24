import qualified Data.Map as M
import Frak

import Test.HUnit
import Test.Tasty
import Test.Tasty.HUnit

bAndBat = Trie {char = Nothing, terminal = False, children = M.fromList [('b',Trie {char = Just 'b', terminal = True, children = M.fromList [('a',Trie {char = Just 'a', terminal = False, children = M.fromList [('t',Trie {char = Just 't', terminal = True, children = M.fromList []})]})]})]}

tests = testGroup "Tests"
    [ testGroup "Trie tests"
      [u "equal? 1"
       (insert "bat" $ insert "b" empty)
       bAndBat]]
    where
      u name real ideal = testCase name (assertEqual "" real ideal)

main = defaultMain tests
