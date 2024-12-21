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

(defonce url-search-params (js/URLSearchParams. (.. js/window -location -search)))

(println (.get url-search-params "abc"))

(defonce interval (r/atom nil))

(defonce gbl-state (r/atom (ls-read :gbl-state defaults/default-state)))
(println @gbl-state)

(def update-gbl-state-in! #(swap! gbl-state assoc-in %1 %2))
(def reset-gbl-state #(reset! gbl-state defaults/default-state))

(def ls-write-gbl-state! #(ls-write :gbl-state @gbl-state))

(defn- calc []
  (let [{:keys [chipset no-of-players start-blind blind-multiplier
                est-game-hours]} @gbl-state
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
        levels-needed (/ (math/log (/ (:val-per-player tdata-total) 2 start-blind)) (math/log (inc (/ blind-multiplier 100.0))))
        est-blind-mins (/ (* est-game-hours 60) levels-needed)]
    {:tdata tdata
     :tdata-total tdata-total
     :levels-needed levels-needed
     :est-blind-mins est-blind-mins}))

(defn- blind-calc [starting-blind blind-level blind-multiplier]
  (if (>= 1 blind-level) starting-blind
      (let [new-blind (* starting-blind (inc (/ blind-multiplier 100)))
            rnd-new-blind (cond (< new-blind 8.5) (math/round new-blind)
                                (< new-blind 50)  (* (math/round (/ new-blind 5)) 5)
                                (< new-blind 500)  (* (math/round (/ new-blind 10)) 10)
                                (< new-blind 1000)  (* (math/round (/ new-blind 50)) 50)
                                :else (* (math/round (/ new-blind 100)) 100))]
        (blind-calc rnd-new-blind (dec blind-level) blind-multiplier))))

(defn- run-time-loop! []
  (let [{:keys [start-blind blind-multiplier game-start]} @gbl-state
        {:keys [est-blind-mins]} (calc)
        anony (fn []
                (let [current-level (math/ceil (/ (- (js/Date.) game-start) 60000 est-blind-mins))]
                  (update-gbl-state-in! [:current-level] current-level)
                  (update-gbl-state-in! [:current-small-blind] (blind-calc start-blind current-level blind-multiplier))))]
    (when-not (nil? @interval) (.clearInterval js/window @interval))
    (reset! interval (.setInterval js/window anony 5000))))

(defn- kill-time-loop! []
  (when-not (nil? @interval) (.clearInterval js/window @interval)))

(defn- gm-start []
  (update-gbl-state-in! [:game-state] :started)
  (update-gbl-state-in! [:game-start] (js/Date.))
  (ls-write-gbl-state!)
  (run-time-loop!))
(defn- gm-pause []
  (update-gbl-state-in! [:game-state] :paused)
  (ls-write-gbl-state!))
(defn- gm-end []
  (update-gbl-state-in! [:game-state] :ended)
  (update-gbl-state-in! [:game-start] nil)
  (kill-time-loop!)
  (ls-write-gbl-state!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn int-input
  ([location val] (int-input location val {}))
  ([location val attrs]
   (let [on-change #(update-gbl-state-in! location %1)]
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
   (let [on-change #(update-gbl-state-in! location %1)]
     [:input (into {:id location
                    :type "number"
                    :value val
                    :on-change #(on-change (-> %1 (.-target) (.-value) float))
                    :style {:width "100%"}}
                   attrs)])))

(defn- game-setup-table [tdata tdata-total]
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
         (td val-left)])]]))

(defn home-page []
  (let [{:keys [no-of-players start-blind blind-multiplier
                est-game-hours game-state game-start
                current-small-blind current-level]} @gbl-state
        {:keys [tdata tdata-total levels-needed est-blind-mins]} (calc)
        game-running (contains? #{:started :paused} (keyword game-state))
        started-at (when-not (nil? game-start) (str (.getHours game-start) ":" (.getMinutes game-start)))]
    [:div
     [:div.row
      [:div.col-md4
       [:div.row [:span {:style {:font-size 20 :font-weight "bold"}} "Poker game tracker! "]]
       [:div.row [:span {:style {:font-size 14 :font-weight "bold"}} "Started at " started-at]]]
      [:div.col-md3
       [:div.row {:style {:padding "4px 10px"}}
        [:div.col-md6 [:button.gm-button
                       {:style {:background-color (when-not game-running "#00ff00")} :on-click gm-start :disabled game-running}
                       "START"]]
        [:div.col-md6 [:button.gm-button
                       {:style {:background-color (when game-running "#ff0000")} :on-click gm-end :disabled (not game-running)}
                       "END"]]]]
      [:div.col-md4 [:div.row.right
                     [:div.col-md8.right
                      [:span {:style {:font-size 14 :font-weight "bold"}} "Small Blind "]
                      [:span {:style {:font-size 26 :font-weight "bold"}} current-small-blind]]
                     [:div.col-md4.right
                      [:span {:style {:font-size 14 :font-weight "bold"}} "Level "]
                      [:span {:style {:font-size 26 :font-weight "bold"}} current-level]]]]]



     (when-not game-running
       [:div.row (game-setup-table tdata tdata-total)])
     (when-not game-running
       [:div.row {:style {:margin-top 10 :width 400}}
        [:div.row
         [:div.col-md6 [:label {:for :no-of-players} "No of Players"]]
         [:div.col-md6 (int-input [:no-of-players] no-of-players {:class "game-setup-input"
                                                                  :min 2
                                                                  :step 1
                                                                  :max 10})]]
        [:div.row
         [:div.col-md6 [:label {:for :start-blind} "Starting SB"]]
         [:div.col-md6 (int-input [:start-blind] start-blind {:class "game-setup-input"})]]
        [:div.row
         [:div.col-md6 [:label {:for :blind-multiplier} "SB Inc percentage"]]
         [:div.col-md6 (num-input [:blind-multiplier] blind-multiplier {:class "game-setup-input"
                                                                        :min 5
                                                                        :step 5
                                                                        :max 300})]]
        [:div.row
         [:div.col-md6 [:label {:for :starting-stack} "Starting Stack"]]
         [:div.col-md6>span#starting-stack (:val-per-player tdata-total)]]
        [:div.row
         [:div.col-md6 [:label {:for :levels-needed} "Levels Needed"]]
         [:div.col-md6>span#levels-needed (math/ceil levels-needed)]]
        [:div.row
         [:div.col-md6 [:label {:for :est-game-hours} "Est Game Time (hours)"]]
         [:div.col-md6 (num-input [:est-game-hours] est-game-hours {:class "game-setup-input"
                                                                    :min 0.75
                                                                    :step 0.25
                                                                    :max 4.00})]]
        [:div.row
         [:div.col-md6 [:label {:for :est-blind-mins} "Est Blind Time (mins)"]]
         [:div.col-md6>span#est-blind-mins (math/ceil est-blind-mins)]]])]))

(defn mount-root []
  (rdom/render [home-page] (js/document.getElementById "app")))

(defn ^:export init! []
  (mount-root))
