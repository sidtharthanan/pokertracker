(ns pokertracker.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [pokertracker.configs :as defaults]))

;; -------------------------

(defonce gbl-state (r/atom (or
                            (.getItem (.-localStorage js/window) :gbl-state)
                            defaults/default-state)))

(println @gbl-state)

;; -------------------------

;; -------------------------
;; Views

(defonce timer
  (r/atom (js/Date.)))

(defonce time-color (r/atom
                     (.getItem (.-localStorage js/window) :color)))

(defonce time-updater (js/setInterval
                       #(reset! timer (js/Date.)) 1000))

(defn greeting [message]
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

(defn home-page []
  (let [{:keys [chipset no-of-players]} @gbl-state
        tdata (map (fn [{:keys [denom color count per-player]}]
                     {:chip denom
                      :color color
                      :total-qty count
                      :total-val (* denom count)
                      :qty-per-player per-player
                      :val-per-player (* denom per-player)
                      :qty-used (* no-of-players per-player)
                      :qty-left (- count (* no-of-players per-player))
                      :val-left (* denom (- count (* no-of-players per-player)))}) chipset)]
    [:div
     [greeting "Hello world, it is now. Yeah"]
     [clock]
     [color-input]
     [:table {:style {:width 800 :border "1px solid black"}}
      [:thead
       [:tr
        [:th {:style {:border "1px solid black"}} "Chip"]
        [:th {:style {:border "1px solid black"}} "Total Qty"]
        [:th {:style {:border "1px solid black"}} "Total Value"]
        [:th {:style {:border "1px solid black"}} "Qty / Player"]
        [:th {:style {:border "1px solid black"}} "Value / Player"]
        [:th {:style {:border "1px solid black"}} "Qty Used"]
        [:th {:style {:border "1px solid black"}} "Qty Left"]
        [:th {:style {:border "1px solid black"}} "Value Left"]]]
      [:tbody
       (map (fn [{:keys [:chip :total-qty :total-val :qty-per-player :val-per-player :qty-used :qty-left :val-left]}]
              [:tr {:key chip}
               [:td {:style {:border "1px solid black"}} chip]
               [:td {:style {:border "1px solid black"}} total-qty]
               [:td {:style {:border "1px solid black"}} total-val]
               [:td {:style {:border "1px solid black"}} qty-per-player]
               [:td {:style {:border "1px solid black"}} val-per-player]
               [:td {:style {:border "1px solid black"}} qty-used]
               [:td {:style {:border "1px solid black"}} qty-left]
               [:td {:style {:border "1px solid black"}} val-left]]) tdata)]
      [:tfoot
       [:tr
        [:td {:style {:border "1px solid black"}} "Total"]
        [:td {:style {:border "1px solid black"}} (reduce + (map :total-qty tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + (map :total-val tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + (map :qty-per-player tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + (map :val-per-player tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + (map :qty-used tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + (map :qty-left tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + (map :val-left tdata))]]]]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [home-page] (js/document.getElementById "app")))

(defn ^:export init! []
  (mount-root))
