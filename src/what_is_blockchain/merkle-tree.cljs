(ns what-is-blockchain.merkle-tree
  (:require
   [cljsjs.web3]
   [cljs-web3.core :as web3]
   [clojure.string :as str]))

(defn tree-from [input]
  (if-let [chunks (seq (str/split input ""))]
    (map web3/sha3 chunks)))
