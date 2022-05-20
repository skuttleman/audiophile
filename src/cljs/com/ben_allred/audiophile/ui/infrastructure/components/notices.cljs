(ns com.ben-allred.audiophile.ui.infrastructure.components.notices
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.store.core :as store]
    [com.ben-allred.audiophile.ui.infrastructure.store.mutations :as mut]))

(defn ^:private banner-message [{:keys [store]} {:keys [banner]}]
  (let [{banner-id :id :keys [body level]} banner
        remove-banner! (fn []
                         (store/dispatch! store (mut/remove-banner! banner-id))
                         nil)]
    (if-not body
      (remove-banner!)
      [:li.banner-message.message
       {:class    [(case level
                     :success "is-success"
                     :error "is-danger"
                     :warning "is-warning"
                     "is-info")]
        :on-click (fn [_]
                    (remove-banner!))
        :style    {:cursor :pointer}}
       [:div.message-header
        [:button.delete {:aria-label "delete"}]]
       [:div.message-body
        [:div.body-text body]]])))

(defn banners [sys {:keys [banners]}]
  [:div.banner-container
   [:ul.banner-messages
    (for [[banner-id banner] (take 2 (sort-by key banners))]
      ^{:key banner-id}
      [banner-message sys {:banner banner}])]])
