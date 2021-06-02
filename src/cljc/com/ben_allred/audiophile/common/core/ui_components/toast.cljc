(ns com.ben-allred.audiophile.common.core.ui-components.toast
  (:require
    [com.ben-allred.audiophile.common.core.ui-components.core :as comp]))

(defn ^:private banner-message [*banners err-codes banner-id {:keys [body level]}]
  (let [body (or (cond-> body
                   (keyword? body) err-codes)
                 "an unknown error has occurred")]
    [:li.banner-message.message
     {:class    [(get {:success "is-success"
                       :error   "is-danger"
                       :warning "is-warning"
                       :info    "is-info"}
                      level)]
      :on-click (fn [_]
                  (comp/remove! *banners banner-id))
      :style    {:cursor :pointer}}
     [:div.message-header
      [:button.delete {:aria-label "delete"}]]
     [:div.message-body
      [:div.body-text body]]]))

(defn ^:private toast-message [_*toasts _toast-id _toast]
  (let [height (volatile! nil)]
    (fn [*toasts toast-id {:keys [body level state]}]
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
          {:on-click (fn [_]
                       (comp/remove! *toasts toast-id))
           :style    {:cursor :pointer}}
          [:div.body-text @body]]]))))

(defn banners [{:keys [*banners err-codes]}]
  (fn [banners]
    [:div.banner-container
     [:ul.banner-messages
      (for [[banner-id banner] (take 2 (sort-by key banners))]
        ^{:key banner-id}
        [banner-message *banners err-codes banner-id banner])]]))

(defn toasts [{:keys [*toasts]}]
  (fn [toasts]
    [:div.toast-container
     [:ul.toast-messages
      (for [[toast-id toast] (take 2 (sort-by key toasts))]
        ^{:key toast-id}
        [toast-message *toasts toast-id toast])]]))
