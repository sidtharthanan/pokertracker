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
     [:input (into {:id location
                    :type "number"
                    :value val
                    :min 0
                    :step 1
                    :on-change #(on-change (-> %1 (.-target) (.-value) int))
                    :style {:width "100%"}}
                   attrs)])))

;; fmt #(-> % float (/ 5) math/round (* 5))

(defn num-input
  ([location val] (num-input location val {}))
  ([location val attrs]
   (let [on-change #(swap! gbl-state assoc-in location %1)]
     [:input (into {:id location
                    :type "number"
                    :value val
                    :on-change #(on-change (-> %1 (.-target) (.-value) float))
                    :style {:width "100%"}}
                   attrs)])))

(defn home-page []
  (let [{:keys [chipset no-of-players start-blind blind-multiplier est-game-hours]} @gbl-state
        tdata (map (fn [{:keys [:denom :color :qty :qty-per-player]}]
                     {:denom denom
                      :color color
                      :qty qty
                      :total-val (* denom qty)
                      :qty-per-player qty-per-player
                      :val-per-player (* denom qty-per-player)
                      :qty-used (* no-of-players qty-per-player)
                      :qty-left (- qty (* no-of-players qty-per-player))
                      :val-left (* denom (- qty (* no-of-players qty-per-player)))}) chipset)
        tdata-total (->> [:qty :qty-per-player :val-per-player :total-val :qty-used :qty-left :val-left]
                         (map (fn [k] [k (reduce + (map k tdata))]))
                         (into {}))
        levels-needed (math/ceil (/ (math/log (/ (:val-per-player tdata-total) 2 start-blind)) (math/log (inc (/ blind-multiplier 100.0)))))
        est-blind-mins (math/ceil (/ (* est-game-hours 60) levels-needed))]
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
         (let [{:keys [:qty :qty-per-player :val-per-player :total-val :qty-used :qty-left :val-left]} tdata-total]
           [:tr
            (td {:style {:font-weight "bold"}} "Total")
            (td qty)
            (td qty-per-player)
            (td {:style {:border "none"}})
            (td val-per-player)
            (td total-val)
            (td qty-used)
            (td qty-left)
            (td val-left)])]])
     [:form {:style {:margin-top 10 :width 400} :disabled true}
      [:div.row
       [:div.col-md5 [:label {:for :no-of-players} "No of Players"]]
       [:div.col-md5 (int-input [:no-of-players] no-of-players {:class "game-setup-input"
                                                                :min 2
                                                                :step 1
                                                                :max 10})]]
      [:div.row
       [:div.col-md5 [:label {:for :start-blind} "Starting SB"]]
       [:div.col-md5 (int-input [:start-blind] start-blind {:class "game-setup-input"})]]
      [:div.row
       [:div.col-md5 [:label {:for :blind-multiplier} "SB Inc percentage"]]
       [:div.col-md5 (num-input [:blind-multiplier] blind-multiplier {:class "game-setup-input"
                                                                      :min 25
                                                                      :step 5
                                                                      :max 100})]]
      [:div.row
       [:div.col-md5 [:label {:for :starting-stack} "Starting Stack"]]
       [:div.col-md5>span#starting-stack (:val-per-player tdata-total)]]
      [:div.row
       [:div.col-md5 [:label {:for :levels-needed} "Levels Needed"]]
       [:div.col-md5>span#levels-needed levels-needed]]
      [:div.row
       [:div.col-md5 [:label {:for :est-game-hours} "Est Game Time (hours)"]]
       [:div.col-md5 (num-input [:est-game-hours] est-game-hours {:class "game-setup-input"
                                                                  :min 0.75
                                                                  :step 0.25
                                                                  :max 4.00})]]
      [:div.row
       [:div.col-md5 [:label {:for :est-blind-mins} "Est Blind Time (mins)"]]
       [:div.col-md5>span#est-blind-mins est-blind-mins]]
      [:button {:type "submit" :on-click persist-gbl-state} "Save locally"]]]))

(defn mount-root []
  (rdom/render [home-page] (js/document.getElementById "app")))

(defn ^:export init! []
  (mount-root))
