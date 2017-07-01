(ns blockchain-web-components.core
  (:require [reagent.core :as reagent]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [cljsjs.d3 :as d3]
            [blockchain-web-components.merkle-tree :as m]))

(enable-console-print!)

(def next-node-id 0)
(defn get-next-node-id []
  (set! next-node-id (inc next-node-id))
  next-node-id)

(defn input-from-state [state]
  (state :input))
(defn tree-from-state [state]
  (state :tree))

(defn state-from [input]
  (if (str/blank? input)
    {:tree nil
     :input input}
    {:tree (m/tree-from (str/split input ""))
     :input input}))

(defn state-update [_ input]
  (state-from input))

(defn input-with-fn [state valuer on-changer]
  [:input {:type "text"
           :value (valuer @state)
           :on-change #(swap! state on-changer (-> % .-target .-value))}])

(defn merkle-tree-input [state]
  [:div#blockchain-merkle-tree-input
   [input-with-fn state input-from-state state-update]])

(def margin (js-obj "top" 40 "right" 90 "bottom" 50 "left" 90))
;; {:width (.-innerWidth js/window)
;;  :height (.-innerHeight js/window)}
(def svg-width (- 660 (.-left margin) (.-right margin)))
(def svg-height (- 500 (.-top margin) (.-bottom margin)))

(defn- treemap [width height]
  (let [t (js/d3.tree.)]
    (.size t (clj->js [width height]))))

(defn build-nodes [tree width height]
  (let [root (-> js/d3
                 (.hierarchy (clj->js tree)))]
    ((treemap width height) root)))

(defn link-position-data [d]
  (str "M" (.-x d) "," (.-y d)
       "C"  (.-x d) "," (/ (+ (.-y d) (.-y (.-parent d))) 2)
       " "  (.-x (.-parent d)) "," (/ (+ (.-y d) (.-y (.-parent d))) 2)
       " "  (.-x (.-parent d)) "," (.-y (.-parent d))))

(defn node-class [d]
  (str "node" " " (if (.-children d)
                    "node--internal"
                    "node--leaf")))

(defn node-translate [d]
  (str "translate(" (.-x d) "," (.-y d) ")"))

(defn- node-text-y [d]
  (if (.-children d)
    -20
    20))

(defn- node-text [d]
  ;; just show first few of hash
  ;; (let [s (subs (.-value (.-data d)) 0 8)]
  (let [s (.-value (.-data d))]
    (if (str/blank? s)
      "\"\""
      s)))

(defn build-transform [margin x y]
  (str "translate(" (x margin) "," (y margin) ")"))

(defn build-link [node]
  [:path
   {:class "link"
    :d (link-position-data node)
    :key (get-next-node-id)}])
(defn build-links [nodes]
  (map build-link (.. nodes
                      descendants
                      (slice 1))))

(defn build-node-text [node]
  [:g
   {:class (node-class node)
    :key (get-next-node-id)
    :transform (node-translate node)}
   [:circle
    {:r 10}]
   [:text
    {:dy "0.35em"
     :y (node-text-y node)
     :style {:text-anchor "middle"}}
    (node-text node)]])

(defn build-nodes-text [nodes]
  (map build-node-text (.descendants nodes)))

(defn render-tree [tree width height]
  (let [nodes (build-nodes tree width height)]
    [:svg
     {:width (+ width (.-left margin) (.-right margin))
      :height (+ height (.-top margin) (.-bottom margin))}
     [:g
      {:transform (build-transform margin #(.-left %) #(.-top %))}
      (build-links nodes)
      (build-nodes-text nodes)]]))

(defn merkle-tree-view [state]
  [:div#blockchain-merkle-tree-view
   (if-let [tree (tree-from-state @state)]
     (render-tree tree svg-width svg-height))])

(defn merkle-tree-component [seed-state]
  (let [state (reagent/atom seed-state)]
    [:div
     [merkle-tree-input state]
     [merkle-tree-view state]]))

(def empty-state {:input ""})

(defn ^:export main []
  (reagent/render-component [merkle-tree-component empty-state]
                            (. js/document (getElementById "app")))
  )
(main)

(comment

  ;; (reagent/create-class
  ;;  {:component-did-update render-tree
  ;;   :render
  ;; [:div#body

  ;;    :r 10}]
  ;;  [:circle
  ;;   {:cx 80
  ;;    :cy 60
  ;;    :r 10}]
  ;;  [:circle
  ;;   {:cx 120
  ;;    :cy 60
  ;;    :r 10}]
  ;;  ])
  ;; (reagent/create-class
  ;;  {:component-did-update update-test
  ;;   :render (render-test state)}))

  ;; (defn- update-test [this old-argv]
  ;;   )
  ;; (defn- render-test [state]
  ;;   @state
  ;;   (fn [this]
  ;;   [:svg
  ;;    {:width 720
  ;;     :height 120}
  ;;    [:circle
  ;;     {:cx 40
  ;;      :cy 60
  ;;      :r 10}]
  ;;    [:circle
  ;;     {:cx 80
  ;;      :cy 60
  ;;      :r 10}]
  ;;    [:circle
  ;;     {:cx 120
  ;;      :cy 60
  ;;      :r 10}]
  ;;    ]))

  ;; (.. js/d3
  ;;     (selectAll "circle")
  ;;     (attr "cx" #(* 720 (.random js/Math))))

  ;; (defn- empty-canvas []
  ;;   [:svg
  ;;    {:width svg-width
  ;;     :height svg-height}
  ;;    [:g
  ;;     {:transform }]])

  ;; (defn display-tree [tree]
  ;;   (if tree
  ;;     (render-tree tree)
  ;;     (empty-canvas)))

  ;; (reagent/create-class
  ;;  {:component-did-update update-test
  ;;   :render (render-test state)}))

  ;; (defn- update-test [this old-argv]
  ;;   )
  ;; (defn- render-test [state]
  ;;   @state
  ;;   (fn [this]

  ;; (defn- svg [width height margin] (-> js/d3
  ;;                                      (.select "svg")
  ;;                                      ;; (.append "svg")
  ;;                                      (.attr "width" (+ width (.-left margin) (.-right margin)))
  ;;                                      (.attr "height" (+ height (.-top margin) (.-bottom margin)))))

  ;; (defn- g [svg margin] (-> svg
  ;;                           (.append "g")
  ;;                           (.attr "transform" (str "translate(" (.-left margin) "," (.-top margin) ")"))))


  ;; (defn link [g nodes] (-> g
  ;;                          (.selectAll ".link")
  ;;                          (.data (-> nodes
  ;;                                     (.descendants)
  ;;                                     (.slice 1)))
  ;;                          (.enter)
  ;;                          (.append "path")
  ;;                          (.attr "class" "link")
  ;;                          (.attr "d" link-position-data)))

  ;; (defn node [g nodes] (-> g
  ;;                          (.selectAll ".node")
  ;;                          (.data (-> nodes
  ;;                                     (.descendants)))
  ;;                          (.enter)
  ;;                          (.append "g")
  ;;                          (.attr "class" node-class)
  ;;                          (.attr "transform" node-translate)))

  (defn- add-circles [node]
    (-> node
        (.append "circle")
        (.attr "r" 10)))
  (defn- add-text [node]
    (-> node
        (.append "text")
        (.attr "dy" ".35em")
        (.attr "y" node-text-y)
        (.style "text-anchor" "middle")
        (.text node-text)))



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
