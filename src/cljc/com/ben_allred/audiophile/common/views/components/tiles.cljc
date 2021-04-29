(ns com.ben-allred.audiophile.common.views.components.tiles
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [integrant.core :as ig]))

(defn tile [heading body & tabs]
  [:div.tile
   ^{:data-foo "bar"} [:div]
   [:div {:data-foo "bar"}]
   [:div.panel {:style {:min-width        "400px"
                        :background-color "#fcfcfc"}}
    (when heading
      [:div.panel-heading
       heading])
    (when (seq tabs)
      (into [:div.panel-tabs {:style {:padding "8px 0"}}] tabs))
    [:div.panel-block
     body]]])

(defmethod ig/init-key ::with-resource [_ {:keys [resource title]}]
  (fn [state view & tabs]
    (into [tile
           [:h2.subtitle title]
           [comp/with-resource [resource {:spinner/size :small}] view state]]
          tabs)))
