(ns what-is-blockchain.merkle-tree
  (:require
   [cljsjs.web3]
   [cljs-web3.core :as web3]
   [clojure.walk :as w]
   [clojure.string :as str]))

(defn rand-str [len]
  "mainly for testing"
  (apply str (take len (repeatedly #(js/String.fromCharCode (+ (rand 26) 97))))))
(defn input [n]
  "mainly for testing"
  (vec (take n (map #(js/String.fromCharCode (+ 97 %)) (range 0 n)))))

(defn the-hash [s]
  (web3/sha3 s))

(defn- hash-children [c]
  (the-hash
   (str/join (mapcat #(% :value) c))))

(defn- tree-partition [input k] (partition k k input))

(defn- make-node [n]
  {:children n})

(defn- make-tree
  "Makes a tree of `branching-factor` with `input` at its leaves.
   Assumes log with some base b of (count input) is `branching-factor` where b is an integer => resulting tree is complete."
  [input branching-factor]
  (make-node
   (if (= branching-factor (count input))
     (map (partial hash-map :value) input)
     (map #(make-tree % branching-factor) (tree-partition input branching-factor)))))

(defn- hash-node [node]
  (if (contains? node :children)
    (assoc node :value (hash-children (node :children)))
    node))

(defn- tree-from-input [input branching-factor]
  (let [tree (make-tree input branching-factor)]
    (w/postwalk hash-node tree)))

(defn- pad [input n empty]
  "append empty to input until it's length is a power of n"
  (let [i (count input)]
    (cond
      (= i 0) nil
      (> (mod i n) 0) (pad (conj input empty) (dec n) empty)
      :else input)))

(def empty-input "")
(defn tree-from
  ([input]
   (tree-from input 2 empty-input))
  ([input n]
   (tree-from input n empty-input))
  ([input n empty-input]
   "produces a merkle tree from input with branching factor n"
   (when-let [padded-input (pad input n empty-input)]
     (tree-from-input padded-input n))))
