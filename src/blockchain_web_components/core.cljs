(ns blockchain-web-components.core
  (:require [reagent.core :as reagent]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [blockchain-web-components.merkle-tree :as m]))

(enable-console-print!)

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

(defn rand-str [len]
  (apply str (take len (repeatedly #(js/String.fromCharCode (+ (rand 26) 97))))))
(defn- keyify-node [node]
  "React requests a unique key on list or repeated elements; helps performance"
  (with-meta [:div node] {:key (rand-str 7)}))

(defn- print-tree [tree]
  (map keyify-node (str/split (clojure.pprint/write tree :stream nil) "\n")))

(defn display-tree [tree]
  (if tree
    (print-tree tree)
    ""))

(defn merkle-tree-view [state]
  (let [tree (tree-from-state @state)]
    [:div#blockchain-merkle-tree-view
     (display-tree tree)]))

(defn merkle-tree-component [seed-state]
  (let [state (reagent/atom seed-state)]
    [:div
     [merkle-tree-input state]
     [merkle-tree-view state]]))

(def empty-state {:input ""})

(defn ^:export main []
  (reagent/render-component [merkle-tree-component empty-state]
                            (. js/document (getElementById "app"))))
(main)

(comment
  ;; TODO defonce state
  ;; following from reagent tutorial
  (defn simple-component []
    [:div
     [:p "I am a component!"]
     [:p.someclass
      "I have " [:strong "bold"]
      [:span {:style {:color "red"}} " and red "] "text."]])

  (defn simple-parent []
    [:div
     [:p "I include simple-component."]
     [simple-component]])

  (defn hello-component [name]
    [:p "Hello, " name "!"])

  (defn say-hello []
    [hello-component "world"])

  (defn lister [items]
    [:ul
     (for [item items]
       ^{:key item} [:li "Item " item])])

  (defn lister-user []
    [:div
     "Here is a list:"
     [lister (range 3)]])

  (def click-count (reagent/atom 0))

  (defn counting-component []
    [:div
     "The atom " [:code "click-count"] " has value: "
     @click-count ". "
     [:input {:type "button" :value "Click me!"
              :on-click #(swap! click-count inc)}]])

  (defn timer-component []
    (let [seconds-elapsed (reagent/atom 0)]
      (fn []
        (js/setTimeout #(swap! seconds-elapsed inc) 1000)
        [:div
         "Seconds Elapsed: " @seconds-elapsed])))
  )
