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


(defn update-gbl-state [[& path] value]
  (swap! gbl-state assoc-in path value))

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

(defn int-input [id location value]
  [:input.config-table-cell {:type "number"
                             :id id
                             :value value
                             :on-change (fn [e] (update-gbl-state location (-> e (.-target) (.-value) int)))
                             :style {:width "100%"
                                     :font-size "1em"
                                     :background-color "inherit"
                                     :color "inherit"
                                     :border "none"}}])

(defn mul-map [[& fns] xs]
  (reduce (fn [acc x] []) [] xs))

(defn home-page []
  (let [{:keys [chipset no-of-players sample]} @gbl-state
        tdata (map (fn [{:keys [denom color count per-player]}]
                     {:chip denom
                      :color color
                      :total-qty count
                      :total-val (* denom count)
                      :qty-per-player per-player
                      :val-per-player (* denom per-player)
                      :qty-used (* no-of-players per-player)
                      :qty-left (- count (* no-of-players per-player))
                      :val-left (* denom (- count (* no-of-players per-player)))}) chipset)
        ;; tfdata (reduce (fn [acc tr]
        ;;                  (assoc acc :total-qty :total-val :qty-per-player :val-per-player :qty-used :qty-left :val-left)) {} tdata)
        ]
    [:div
     [greeting "Hello world, it is now. Yeah"]
     [clock]
     [color-input]
     [:table {:style {:width 800 :border "1px solid black"}}
      [:thead
       [:tr
        [:th {:style {:border "1px solid black"}} "Denom"]
        [:th {:style {:border "1px solid black"}} "Qty"]
        [:th {:style {:border "1px solid black"}} "Denom * Qty"]
        [:th {:style {:border "1px solid black"}} "Qty / Player"]
        [:th {:style {:border "1px solid black"}} "Value / Player"]
        [:th {:style {:border "1px solid black"}} "Qty Used"]
        [:th {:style {:border "1px solid black"}} "Qty Left"]
        [:th {:style {:border "1px solid black"}} "Value Left"]]]
      [:tbody
       (map-indexed (fn [index {:keys [:color :chip :total-qty :total-val :qty-per-player :val-per-player :qty-used :qty-left :val-left]}]
                      [:tr {:key chip :style {:background color :color "white"}}
                       [:td {:style {:border "1px solid black" :width "5%"}} chip]
                       [:td {:style {:border "1px solid black" :width "10%"}} (int-input chip [:chipset index :count] total-qty)]
                       [:td {:style {:border "1px solid black"}} total-val]
                       [:td {:style {:border "1px solid black"}} qty-per-player]
                       [:td {:style {:border "1px solid black"}} val-per-player]
                       [:td {:style {:border "1px solid black"}} qty-used]
                       [:td {:style {:border "1px solid black"}} qty-left]
                       [:td {:style {:border "1px solid black"}} val-left]]) tdata)]
      [:tfoot
       [:tr
        [:td {:style {:border "1px solid black" :font-weight "bold"}} "Total"]
        [:td {:style {:border "1px solid black"}} (reduce + 0 (map :total-qty tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + 0 (map :total-val tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + 0 (map :qty-per-player tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + 0 (map :val-per-player tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + 0 (map :qty-used tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + 0 (map :qty-left tdata))]
        [:td {:style {:border "1px solid black"}} (reduce + 0 (map :val-left tdata))]]]]
     (int-input 101 [:sample] sample)
     [:input {:field :text :id :first-name}]
     [:input {:field :numeric :id :age}]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [home-page] (js/document.getElementById "app")))

(defn ^:export init! []
  (mount-root))
