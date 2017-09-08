(ns blockchain-web-components.merkle-tree
  (:require
   [cljsjs.web3]
   [cljs-web3.core :as web3]
   [clojure.walk :as w]
   [goog.string :as gstring]
   [goog.string.format]
   [clojure.string :as str]))

(defn- rand-str [len]
  "mainly for testing"
  (apply str (take len (repeatedly #(js/String.fromCharCode (+ (rand 26) 97))))))
(defn- input [n]
  "mainly for testing"
  (vec (take n (map #(js/String.fromCharCode (+ 97 %)) (range 0 n)))))

(defn- hasher [s]
  (let [repr (str/join "" (map :value s))]
    (web3/sha3 repr)))

(defn- insert-tree [t [[first & rest] v]]
  (cond
    (= first "0") (assoc t :left  (insert-tree (:left t) [rest v]))
    (= first "1") (assoc t :right (insert-tree (:right t) [rest v]))
    :else (assoc t :value v)))

(defn- bitstring [i total]
  ;; gstring lacks binary repr
  (let [s (.toString i 2)]
    (gstring/format (str "%0" total "d") s)))

(defn- indexer [total i item]
  [(bitstring i total) item])

(defn- log [base n]
  (/ (.log js/Math n) (.log js/Math base)))
(defn- depth [coll]
  (Math/ceil (log 2 (count coll))))

(defn- build-tree
  ([coll]
   (let [d (depth coll)]
     (build-tree (map-indexed (partial indexer d) coll) {})))
  ([coll t]
   (if-let [s (seq coll)]
     (build-tree (rest s) (insert-tree t (first s)))
     t)))

(defn children [{:keys [left right]}]
  (seq (remove nil? [left right])))
(defn- hash-node [n]
  (if-let [c (children n)]
    (assoc n :value (hasher c))
    n))
(defn- hash-tree [t]
  (w/postwalk hash-node t))

(defn tree-from [coll]
  (hash-tree (build-tree coll)))

(tree-from [])
