(ns blockchain-web-components.render
  (:require [cljsjs.d3 :as d3]
            [clojure.string :as str]))

(def next-node-id 0)
(defn get-next-node-id []
  (set! next-node-id (inc next-node-id))
  next-node-id)

(def margin (js-obj "top" 40 "right" 90 "bottom" 50 "left" 90))
;; {:width (.-innerWidth js/window)
;;  :height (.-innerHeight js/window)}
;; (def svg-width (- 660 (.-left margin) (.-right margin)))
(defn svg-width [] (* 0.8
                  (.-innerWidth js/window)))
;; (def svg-height (- 500 (.-top margin) (.-bottom margin)))
(defn svg-height [] (* 1
                   (.-innerHeight js/window)))

(defn- clustermap [width height]
  (let [t (js/d3.cluster.)]
    (.size t (clj->js [height (- width 160)]))))

(defn- treemap [width height]
  (let [t (js/d3.tree.)]
    (.size t (clj->js [width height]))))

(defn- select-children [d]
  (clj->js (remove nil? (vector (.-left d) (.-right d)))))

(defn pp [obj]
  (.log js/console obj))

(defn build-nodes [tree mapper width height]
  (let [root (.hierarchy js/d3 (clj->js tree) select-children)]
    ((mapper width height) root)))

(defn vertical-link [d]
  (str "M" (.-x d) "," (.-y d)
       "C"  (.-x d) "," (/ (+ (.-y d) (.-y (.-parent d))) 2)
       " "  (.-x (.-parent d)) "," (/ (+ (.-y d) (.-y (.-parent d))) 2)
       " "  (.-x (.-parent d)) "," (.-y (.-parent d))))

(defn horizontal-link [d]
  (str "M" (.-y d) "," (.-x d)
       "C" (+ 100 (-> d
                      .-parent
                      .-y))
       "," (.-x d)
       " " (+ 100 (-> d
                      .-parent
                      .-y))
       "," (-> d
               .-parent
               .-x)
       " " (-> d
               .-parent
               .-y)
       "," (-> d
               .-parent
               .-x)))

(defn node-class [d]
  (str "node"
       " "
       (if (.-children d)
         "node--internal"
         "node--leaf")
       " "
       (if (.-parent d)
         ""
         "node--root")))

(defn- get-x [obj]
  (.-x obj))
(defn- get-y [obj]
  (.-y obj))

(defn node-translate [d orientation]
  (let [fnmap {:vertical [get-x get-y (fn [_] "")]
               :horizontal [get-y get-x (fn [_] "")]}
        data (map #(% d) (fnmap orientation))
        intercalations ["translate(" "," ")"]]
    (str/join ""
      (interleave intercalations data))))

(defn- node-text-y [d]
  (if (.-children d)
    -20
    20))

(defn- node-text [d]
  "if d has children, it is a hash and only reveal an abbreviation"
  (let [s (.-value (.-data d))]
    (if (.-children d)
      (str (subs s 0 12) "...")
      s)))

(defn build-transform [margin x y]
  (str "translate(" (x margin) "," (y margin) ")"))

(defn build-link [node link-position]
  [:path
   {:class "link"
    :d (link-position node)
    :key (get-next-node-id)}])
(defn build-links [nodes link-position]
  (map #(build-link % link-position) (.. nodes
                      descendants
                      (slice 1))))

(defn- radius-for [orientation]
  ({:vertical 10
    :horizontal 5} orientation))

(defn- dy-for [node orientation]
  (case orientation
    :vertical "0.35em"
    :horizontal (if (.-children node)
                  "1.5em"
                  "0")))

(defn- pos-prop-for [orientation]
  ({:vertical :y
    :horizontal :x} orientation))

(defn- horizontal-node-text [node]
  (if (.-children node)
    -8
    8))

(defn- node-text-pos [node orientation]
  (({:vertical node-text-y
     :horizontal horizontal-node-text} orientation) node))

(defn- text-anchor [node orientation]
  (case orientation
    :horizontal (if (.-children node)
                  "middle"
                  "start")
    :vertical "middle"))

(defn- text-attrs [node orientation]
  {:dy                        (dy-for node orientation)
   (pos-prop-for orientation) (node-text-pos node orientation)
   :style {:text-anchor       (text-anchor node orientation)}})

(defn build-node-text [node orientation]
  [:g
   {:class (node-class node)
    :key (get-next-node-id)
    :transform (node-translate node orientation)}
   [:circle
    {:r (radius-for orientation)}]
   [:text
    (text-attrs node orientation)
    (node-text node)]])

(defn build-nodes-text [nodes orientation]
  (map #(build-node-text % orientation) (.descendants nodes)))

(defn render-tree-vertical
  "returns a vertically oriented HTML representation of `tree`"
  ([tree]
   (if (= 0 (count tree))
     [:div]
     (render-tree-vertical tree (svg-width) (svg-height))))
  ([tree width height]
   (let [nodes (build-nodes tree treemap width height)]
     [:svg
      {:width (+ width (.-left margin) (.-right margin))
       :height (+ height (.-top margin) (.-bottom margin))}
      [:g
       {:transform (build-transform margin #(.-left %) #(.-top %))}
       (build-links nodes vertical-link)
       (build-nodes-text nodes :vertical)]])))

(defn render-tree-horizontal
  "returns a horizontally oriented HTML representation of `tree`"
  ([tree]
   (if (= 0 (count tree))
     [:div]
     (render-tree-horizontal tree (svg-width) (svg-height))))
  ([tree width height]
   (let [nodes (build-nodes tree clustermap width height)]
     [:svg
      {:width (+ width (.-left margin) (.-right margin))
       :height (+ height (.-top margin) (.-bottom margin))}
      [:g
       {:transform (build-transform margin #(.-left %) #(.-top %))}
       (build-links nodes horizontal-link)
       (build-nodes-text nodes :horizontal)]])))

(defn render-tree [{:keys [tree orientation]}]
  (({:horizontal render-tree-horizontal
    :vertical    render-tree-vertical} orientation) tree))

(comment
  (defn rand-str [len]
    (apply str (take len (repeatedly #(js/String.fromCharCode (+ (rand 26) 97))))))
  (defn- keyify-node [node]
    "React requests a unique key on list or repeated elements; helps performance"
    (with-meta [:div node] {:key (rand-str 7)}))

  (defn- print-tree [tree]
    (map keyify-node (str/split (clojure.pprint/write tree :stream nil) "\n")))
  (def test-data
    "for testing"
    (clj->js {:name "top level"
              :children [{:name "level 2: A"
                          :children [{:name "Son of A"}
                                     {:name "Daughter of A"}]}
                         {:name "Level 2: B"}]}))

  )
