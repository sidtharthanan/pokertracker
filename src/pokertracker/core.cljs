(ns pokertracker.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [pokertracker.configs :as defaults]))

(defn ls-read
  ([k] (edn/read-string (.getItem (.-localStorage js/window) k)))
  ([k d] (or (ls-read k) d)))

(defn ls-write [k v]
  (.setItem (.-localStorage js/window) k (prn-str v)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce gbl-state (r/atom (ls-read :gbl-state defaults/default-state)))
(println @gbl-state)

(def update-gbl-state-in #(swap! gbl-state assoc-in %1 %2))
(def reset-gbl-state #(reset! gbl-state defaults/default-state))

(def persist-gbl-state #(ls-write :gbl-state @gbl-state))

;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn int-input [id value location]
  [:input.config-table-input {:type "number"
                              :id id
                              :value value
                              :on-change (fn [e] (update-gbl-state-in location (-> e (.-target) (.-value) int)))}])

(defn home-page []
  (let [{:keys [chipset no-of-players]} @gbl-state
        tdata (map (fn [{:keys [:denom :color :qty :qty-per-player]}]
                     {:denom denom
                      :color color
                      :total-qty qty
                      :total-val (* denom qty)
                      :qty-per-player qty-per-player
                      :val-per-player (* denom qty-per-player)
                      :qty-used (* no-of-players qty-per-player)
                      :qty-left (- qty (* no-of-players qty-per-player))
                      :val-left (* denom (- qty (* no-of-players qty-per-player)))}) chipset)]
    [:div
     [:h3 "Poker game tracker!"]
     (let [th (partial vector :th.config-table-cell)
           td (partial vector :td.config-table-cell)]
       [:table {:style {:width 800 :border "1px solid black"}}
        [:thead
         [:tr
          (th "Denom")
          (th "Qty")
          (th "Chip Value")
          (th "Qty / Player")
          (th "Value / Player")
          (th "Qty Used")
          (th "Qty Left")
          (th "Value Left")]]
        [:tbody
         (map-indexed (fn [index {:keys [:color :denom :total-qty :total-val :qty-per-player :val-per-player :qty-used :qty-left :val-left]}]
                        [:tr {:key denom :style {:background color :color "white"}}
                         (td {:style {:width "5%"}} denom)
                         (td {:style {:width "10%"}} (int-input denom total-qty [:chipset index :qty]))
                         (td {:style {}} total-val)
                         (td {:style {:width "15%"}} (int-input denom qty-per-player [:chipset index :qty-per-player]))
                         (td {:style {}} val-per-player)
                         (td {:style {}} qty-used)
                         (td {:style {}} qty-left)
                         (td {:style {}} val-left)]) tdata)]
        [:tfoot
         [:tr
          (td {:style {:font-weight "bold"}} "Total")
          (td (reduce + (map :total-qty tdata)))
          (td (reduce + (map :total-val tdata)))
          (td (reduce + (map :qty-per-player tdata)))
          (td (reduce + (map :val-per-player tdata)))
          (td (reduce + (map :qty-used tdata)))
          (td (reduce + (map :qty-left tdata)))
          (td (reduce + (map :val-left tdata)))]]])
     [:button {:on-click persist-gbl-state} "Save locally"]]))

(defn mount-root []
  (rdom/render [home-page] (js/document.getElementById "app")))

(defn ^:export init! []
  (mount-root))
