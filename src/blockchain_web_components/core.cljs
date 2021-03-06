(ns blockchain-web-components.core
  (:require [reagent.core :as reagent]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [blockchain-web-components.render :as r]
            [blockchain-web-components.merkle-tree :as m]))

(enable-console-print!)

(defn- input [state on-changer]
  [:input {:type "text"
           :value (@state :current-input)
           :on-change #(swap! state on-changer (-> % .-target .-value))}])

(defn- update-state [state new-data]
  (-> state
      (assoc :data new-data)
      (assoc :tree (m/tree-from new-data))))

(defn- add-message [state]
  (let [data (state :data)
        msg (state :current-input)]
    (if (str/blank? msg)
      state
      (let [new-data (conj data msg)]
        (update-state state new-data)))))

(defn- update-input [state input]
  (assoc state :current-input input))

(defn- button [state fn txt]
  [:button
   {:on-click #(swap! state fn)}
   txt])

(defn- merkle-tree-input [state]
  [:div#blockchain-merkle-tree-input
   (input state update-input)
   (button state add-message "Add message")])

(defn- remove-msg-from [i data]
  (let [[head tail] (split-at i data)]
    (into (into [] head) (rest tail))))

(defn- pp [obj]
  (.log js/console (str  obj)))

(defn- remove-msg [state i]
  (let [data (state :data)]
    (update-state state (remove-msg-from i data))))

(defn- listify [i node state]
  [:li
   {:key i}
   node
   (button state #(remove-msg % i) "x")])

(defn- merkle-tree-txn-list [state]
  [:div#blockchain-merkle-tree-txn-list
   [:ul
    (map-indexed #(listify %1 %2 state) (@state :data))]])

(defn- merkle-tree-view [state width height]
  [:div#blockchain-merkle-tree-view
   (let [t (@state :tree)]
     (r/render-tree
      {:tree t
       :orientation :horizontal
       :width width
       :height height}))])

(def empty-state
  (let [data []]
    (-> {}
        (assoc :data data)
        (assoc :tree (m/tree-from data))
        (assoc :current-input ""))))

(defn- show [state]
  [:p
   (str   ;;@state
    (dissoc @state :tree)
    )])

(def test-state
  (let [test-data ["send 1 btc to alex"
                   "send 2 btc to danny"
                   "send 3 btc to alex"
                   "send 4 btc to tycho"]]
    (-> empty-state
        (assoc :data test-data)
        (assoc :tree (m/tree-from test-data)))))

(defn- admin [state]
  [:div#admin
   (show state)
   (button state (fn [_] empty-state) "reset")
   (button state (fn [_] test-state) "test-data")])

(defn- root-hash [{:keys [tree]}]
  (tree :value))

(defn- merkle-tree-result [state]
  [:div#state
   [:p (str "Merkle root hash: " (root-hash @state))]])

;; https://github.com/reagent-project/reagent-cookbook/blob/master/recipes/canvas-fills-div/README.md
(def window-width (reagent/atom nil))

(defn on-window-resize [evt]
  (reset! window-width (.-innerWidth js/window)))

(defn render-with-resize [state]
  (let [dom-node (reagent/atom nil)]
    (reagent/create-class
     {:component-did-update
      (fn [ this ]
        [merkle-tree-view state
         (if-let [node @dom-node]
           (.-clientWidth node))
         (if-let [node @dom-node]
           (.-clientHeight node))])

      :component-did-mount
      (fn [ this ]
        (reset! dom-node (reagent/dom-node this)))

      :reagent-render
      (fn [ ]
        @window-width ;; Trigger re-render on window resizes
        [merkle-tree-view state
         (if-let [node @dom-node]
           (.-clientWidth node))
         (if-let [node @dom-node]
           (.-clientHeight node))])})))

(defn- merkle-tree-component [seed-state]
  (let [state (reagent/atom seed-state)]
    [:div
     [admin state]
     [merkle-tree-input state]
     [merkle-tree-txn-list state]
     [merkle-tree-result state]
     [render-with-resize state]]))

(defn ^:export main []
  (reagent/render-component [merkle-tree-component empty-state]
                            (. js/document (getElementById "app")))
  (.addEventListener js/window "resize" on-window-resize))

(main)
