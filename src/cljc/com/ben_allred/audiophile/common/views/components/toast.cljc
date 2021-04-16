(ns com.ben-allred.audiophile.common.views.components.toast
  (:require
    [com.ben-allred.audiophile.common.services.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [integrant.core :as ig]))

(defn ^:private banner-message [store err-codes banner-id {:keys [body level]}]
  (let [body (or (cond-> body
                   (keyword? body) err-codes)
                 "an unknown error has occurred")]
    [:li.banner-message.message
     {:class [(get {:success "is-success"
                    :error   "is-danger"
                    :warning "is-warning"
                    :info    "is-info"}
                   level)]
      :on-click #(ui-store/dispatch! store (actions/remove-banner! banner-id))
      :style    {:cursor :pointer}}
     [:div.message-header
      [:button.delete {:aria-label "delete"}]]
     [:div.message-body
      [:div.body-text body]]]))

(defmethod ig/init-key ::banners [_ {:keys [err-codes store]}]
  (fn [banners]
    [:div.banner-container
     [:ul.banner-messages
      (for [[banner-id banner] (take 2 (sort-by key banners))]
        ^{:key banner-id}
        [banner-message store err-codes banner-id banner])]]))

(defn ^:private toast-message [_store _toast-id _toast]
  (let [height (volatile! nil)]
    (fn [store toast-id {:keys [body level state]}]
      (let [adding? (= state :init)
            removing? (= state :removing)]
        [:li.toast-message.message
         (cond-> {:ref   (fn [node]
                           (some->> node
                                    (.getBoundingClientRect)
                                    (.-height)
                                    (vreset! height)))
                  :class [({:success "is-success"
                            :error   "is-danger"
                            :warning "is-warning"
                            :info    "is-info"}
                           level)
                          (when adding? "adding")
                          (when removing? "removing")]}
           (and removing? @height) (update :style assoc :margin-top (str "-" @height "px")))
         [:div.message-body
          {:on-click #(ui-store/dispatch! store (actions/remove-toast! toast-id))
           :style    {:cursor :pointer}}
          [:div.body-text @body]]]))))

(defmethod ig/init-key ::toasts [_ {:keys [store]}]
  (fn [toasts]
    [:div.toast-container
     [:ul.toast-messages
      (for [[toast-id toast] (take 2 (sort-by key toasts))]
        ^{:key toast-id}
        [toast-message store toast-id toast])]]))
