(ns pokertracker.configs)

(def ^:export default-state {:chipset [{:denom 1 :color "#c0c0c0" :count 40  :per-player 5}
                                       {:denom 5 :color "red" :count 40  :per-player 4}
                                       {:denom 10 :color "green" :count 40  :per-player 4}
                                       {:denom 25 :color "blue" :count 40  :per-player 3}
                                       {:denom 50 :color "black" :count 40  :per-player 2}]
                             
                             :game-duration-secs (* 2 60 60)
                             :no-of-players 4
                             :start-blind 2
                             :blind-multiplier 1.5})