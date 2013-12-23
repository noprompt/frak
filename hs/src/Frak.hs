module Frak where

import qualified Data.Map as M

data Trie = Trie { char :: Maybe Char
                 , terminal :: Bool
                 , children :: (M.Map Char Trie)
                 } deriving (Show)

empty = Trie Nothing False M.empty

-- Trie (Just 'b') False $ M.fromList [('a', Trie (Just 'a') False $ M.fromList [('t', Trie (Just 't') True M.empty)])]
-- adding b to bat changes b False a False t True
-- to b True a False t True

insert :: String -> Trie -> Trie
insert [] trie = trie
insert (c:cs) (Trie char terminal children) =
  Trie char terminal newChildren 
  where
    term = null cs
    newChildren = case M.lookup c children of
      Nothing ->
        M.insert c (insert cs $ Trie (Just c) term M.empty) children
      Just (Trie _ terminal innerChildren) ->
        M.insert c (insert cs $ Trie (Just c) (terminal || term) innerChildren) children

fromList = foldr insert empty
