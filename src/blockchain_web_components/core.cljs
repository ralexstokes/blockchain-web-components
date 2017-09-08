(ns blockchain-web-components.core
  (:require [reagent.core :as reagent]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [blockchain-web-components.render :as r]
            [blockchain-web-components.merkle-tree :as m]))

(enable-console-print!)

(defn- input [state on-changer]
  [:input {:type "text"
           :on-change #(swap! state on-changer (-> % .-target .-value))}])

(defn- add-message [state]
  (let [data (state :data)
        msg (state :current-input)]
    (if (str/blank? msg)
      state
      (assoc state :data (conj data msg)))))

(defn- update-input [state input]
  (assoc state :current-input input))

(defn- button [state fn txt]
  [:button {:on-click #(swap! state fn)}
   txt])

(defn- merkle-tree-input [state]
  [:div#blockchain-merkle-tree-input
   (input state update-input)
   (button state add-message "Add message")])

(defn- merkle-tree-view [state]
  [:div#blockchain-merkle-tree-view
   (let [data (@state :data)]
     (r/render-tree
      (m/tree-from data)))])

(defn- merkle-tree-component [seed-state]
  (let [state (reagent/atom seed-state)]
    [:div
     [merkle-tree-input state]
     [merkle-tree-view state]]))

(def empty-state {:data []
                  :current-input ""})

(defn ^:export main []
  (reagent/render-component [merkle-tree-component empty-state]
                            (. js/document (getElementById "app"))))

(main)
