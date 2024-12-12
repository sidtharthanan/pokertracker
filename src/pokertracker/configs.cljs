(ns pokertracker.configs)

(def ^:export default-state {:chipset [{:denom 1 :color "#c0c0c0" :qty 40  :qty-per-player 5}
                                       {:denom 5 :color "red" :qty 40  :qty-per-player 4}
                                       {:denom 10 :color "green" :qty 40  :qty-per-player 4}
                                       {:denom 25 :color "blue" :qty 40  :qty-per-player 3}
                                       {:denom 50 :color "black" :qty 40  :qty-per-player 2}]
                             
                             :game-duration-secs (* 2 60 60)
                             :no-of-players 4
                             :start-blind 2
                             :blind-multiplier 50})