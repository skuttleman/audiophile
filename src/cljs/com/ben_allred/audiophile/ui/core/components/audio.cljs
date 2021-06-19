(ns com.ben-allred.audiophile.ui.core.components.audio
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]
    [com.ben-allred.vow.core :as v :include-macros true]))

(defn player [{:keys [player]}]
  (fn [artifact-id]
    (comp/load! player {:artifact-id artifact-id})
    (r/create-class
      {:reagent-render
       (fn [_artifact-id]
         (let [ready? (comp/ready? player)]
           [:div {:style {:width "100%"}}
            (when-not ready?
              [in/spinner {:size :large}])
            [:div.audio-player {:id (comp/id player)}]
            (when ready?
              [:div.buttons
               [in/plain-button {:on-click (fn [_]
                                             (comp/play-pause! player))
                                 :disabled (not ready?)}
                [comp/icon (if (comp/playing? player) :pause :play)]]
               (when (comp/region player)
                 [in/plain-button {:on-click (fn [_]
                                               (comp/set-region! player))}
                  "clear region"])
               (when-not ready?
                 [in/spinner])])]))
       :component-will-unmount
       (fn [_]
         (comp/destroy! player))})))
