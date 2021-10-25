(ns com.ben-allred.audiophile.ui.core.components.toast
  (:require
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]))

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

(defn ^:private toast-message [*toasts toast-id {:keys [body level state]}]
  (r/with-let [height (volatile! nil)]
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
        [:div.body-text @body]]])))

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
