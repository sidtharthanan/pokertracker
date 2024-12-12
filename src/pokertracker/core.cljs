(ns pokertracker.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [pokertracker.configs :as defaults]))

;; -------------------------

(defonce gbl-state (r/atom (or
                            (edn/read-string (.getItem (.-localStorage js/window) :gbl-state))
                            defaults/default-state)))

(println @gbl-state)

;; -------------------------

;; -------------------------
;; Views


(defn update-gbl-state [[& path] value]
  (swap! gbl-state assoc-in path value))

(defn persist-gbl-state []
  (.setItem (.-localStorage js/window) :gbl-state (prn-str @gbl-state)))

(defonce timer
  (r/atom (js/Date.)))

(defonce time-color (r/atom
                     (.getItem (.-localStorage js/window) :color)))

(defonce time-updater (js/setInterval
                       #(reset! timer (js/Date.)) 1000))

(defn greeting [message]
  (println "called greeting")
  [:h1 message])

(defn clock []
  (let [time-str (-> @timer .toTimeString (str/split " ") first)]
    [:div#id1001.example-clock>p>span
     {:style {:color @time-color}}
     time-str]))

(defn color-input []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @time-color
            :on-change (fn [element]
                         (let [new-color (-> element (.-target) (.-value))]
                           (.setItem (.-localStorage js/window) :color new-color)
                           (reset! time-color new-color)))}]])

(defn int-input [id value location]
  [:input.config-table-input {:type "number"
                              :id id
                              :value value
                              :on-change (fn [e] (update-gbl-state location (-> e (.-target) (.-value) int)))}])

(defn home-page []
  (let [{:keys [chipset no-of-players sample]} @gbl-state
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
     [greeting "Hello world, it is now. Yeah"]
     [clock]
     [color-input]
     [:table {:style {:width 800 :border "1px solid black"}}
      [:thead
       [:tr
        [:th.config-table-cell "Denom"]
        [:th.config-table-cell "Qty"]
        [:th.config-table-cell "Chip Value"]
        [:th.config-table-cell "Qty / Player"]
        [:th.config-table-cell "Value / Player"]
        [:th.config-table-cell "Qty Used"]
        [:th.config-table-cell "Qty Left"]
        [:th.config-table-cell "Value Left"]]]
      [:tbody
       (map-indexed (fn [index {:keys [:color :denom :total-qty :total-val :qty-per-player :val-per-player :qty-used :qty-left :val-left]}]
                      [:tr {:key denom :style {:background color :color "white"}}
                       [:td.config-table-cell {:style {:width "5%"}} denom]
                       [:td.config-table-cell {:style {:width "10%"}} (int-input denom total-qty [:chipset index :qty])]
                       [:td.config-table-cell {:style {}} total-val]
                       [:td.config-table-cell {:style {:width "15%"}} (int-input denom qty-per-player [:chipset index :qty-per-player])]
                       [:td.config-table-cell {:style {}} val-per-player]
                       [:td.config-table-cell {:style {}} qty-used]
                       [:td.config-table-cell {:style {}} qty-left]
                       [:td.config-table-cell {:style {}} val-left]]) tdata)]
      [:tfoot
       [:tr
        [:td.config-table-cell {:style {:font-weight "bold"}} "Total"]
        [:td.config-table-cell (reduce + (map :total-qty tdata))]
        [:td.config-table-cell (reduce + (map :total-val tdata))]
        [:td.config-table-cell (reduce + (map :qty-per-player tdata))]
        [:td.config-table-cell (reduce + (map :val-per-player tdata))]
        [:td.config-table-cell (reduce + (map :qty-used tdata))]
        [:td.config-table-cell (reduce + (map :qty-left tdata))]
        [:td.config-table-cell (reduce + (map :val-left tdata))]]]]
     [:button {:on-click persist-gbl-state} "Save locally"]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [home-page] (js/document.getElementById "app")))

(defn ^:export init! []
  (mount-root))
