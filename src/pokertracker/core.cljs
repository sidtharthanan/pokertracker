(ns pokertracker.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.math :as math]
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

(defn int-input
  ([location val] (int-input location val {}))
  ([location val attrs]
   (let [on-change #(swap! gbl-state assoc-in location %1)]
     [:input (assoc attrs
                    :id location
                    :type "number"
                    :value val
                    :min 0
                    :step 1
                    :on-change #(on-change (-> %1 (.-target) (.-value) int)))])))

;; fmt #(-> % float (/ 5) math/round (* 5))

(defn num-input
  ([location val] (num-input location val {}))
  ([location val attrs]
   (let [on-change #(swap! gbl-state assoc-in location %1)]
     [:input (assoc attrs
                    :id location
                    :type "number"
                    :value val
                    :on-change #(on-change (-> %1 (.-target) (.-value) float)))])))

(defn home-page []
  (let [{:keys [chipset no-of-players start-blind blind-multiplier]} @gbl-state
        tdata (map (fn [{:keys [:denom :color :qty :qty-per-player]}]
                     {:denom denom
                      :color color
                      :qty qty
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
       [:table {:style {:width 900 :border "1px solid black"}}
        [:thead
         [:tr
          (th {:style {:width "5%"}} "Denom")
          (th {:style {:width "10%"}} "Qty")
          (th {:style {:width "15%"}} "Qty / Player")
          (th {:style {:width "5%" :border "none"}})
          (th "Starting Stack")
          (th "Chip Value")
          (th "Qty Used")
          (th "Qty Left")
          (th "Value Left")]]
        [:tbody
         (map-indexed (fn [index {:keys [:color :denom :total-val :val-per-player :qty-used :qty-left :val-left :qty :qty-per-player]}]
                        [:tr {:key [:chipset index] :style {:background color :color "white"}}
                         (td  denom)
                         (td  (int-input [:chipset index :qty] qty {:class "config-table-input"}))
                         (td  (int-input [:chipset index :qty-per-player] qty-per-player {:class "config-table-input"}))
                         (td {:style {:border "none"}})
                         (td val-per-player)
                         (td total-val)
                         (td qty-used)
                         (td qty-left)
                         (td val-left)]) tdata)]
        [:tfoot
         [:tr
          (td {:style {:font-weight "bold"}} "Total")
          (td (reduce + (map :qty tdata)))
          (td (reduce + (map :qty-per-player tdata)))
          (td {:style {:border "none"}})
          (td (reduce + (map :val-per-player tdata)))
          (td (reduce + (map :total-val tdata)))
          (td (reduce + (map :qty-used tdata)))
          (td (reduce + (map :qty-left tdata)))
          (td (reduce + (map :val-left tdata)))]]])
     [:form {:style {:margin-top 10 :width 400}}
      [:div.row
       [:div.col-md5 [:label {:for :start-blind} "Starting small blind"]]
       [:div.col-md5 (int-input [:start-blind] start-blind {:class "game-setup-input"})]]
      [:div.row
       [:div.col-md5 [:label {:for :blind-multiplier} "Small Blind Multiplier"]]
       (println blind-multiplier)
       [:div.col-md5 (num-input [:blind-multiplier] blind-multiplier {:class "game-setup-input"
                                                                      :min 30
                                                                      :step 2
                                                                      :max 100})]]
      [:button {:type "submit" :on-click persist-gbl-state} "Save locally"]]]))

(defn mount-root []
  (rdom/render [home-page] (js/document.getElementById "app")))

(defn ^:export init! []
  (mount-root))
