(ns com.ben-allred.audiophile.common.core.ui-components.tiles
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.ui-components.core :as comp]))

(defn tile [heading body & tabs]
  [:div.tile
   [:div.panel {:style {:min-width        "400px"
                        :background-color "#fcfcfc"}}
    (when heading
      [:div.panel-heading
       heading])
    (when (seq tabs)
      (into [:div.panel-tabs
             {:style {:padding         "8px"
                      :justify-content :flex-start}}]
            tabs))
    [:div.panel-block
     body]]])

(defn with-resource [{:keys [*resource title view]}]
  (fn [state & tabs]
    (into [tile
           [:h2.subtitle title]
           [comp/with-resource [*resource {:spinner/size :small}] view state]]
          tabs)))
