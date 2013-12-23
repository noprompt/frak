module Frak where

import qualified Data.Map as M

data Trie = Trie { val :: Maybe Char,
                   isTerminal :: Bool,
                   children :: (M.Map Char Trie)} deriving (Show)

empty = Trie Nothing False M.empty

fromList = undefined
-- Trie (Just 'b') False $ M.fromList [('a', Trie (Just 'a') False $ M.fromList [('t', Trie (Just 't') True M.empty)])]
-- adding b to bat changes b False a False t True
-- to b True a False t True
