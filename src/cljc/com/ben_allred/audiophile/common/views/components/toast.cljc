(ns com.ben-allred.audiophile.common.views.components.toast
  (:require
    [com.ben-allred.audiophile.common.services.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [integrant.core :as ig]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defn ^:private toast-message [_store _err-codes _toast-id _toast]
  (let [height (volatile! nil)]
    (fn [store err-codes toast-id {:keys [body level state]}]
      (let [adding? (= state :init)
            removing? (= state :removing)
            body @body]
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
          [:div.body-text (cond-> body
                            (keyword? body) err-codes)]]]))))

(defmethod ig/init-key ::toasts [_ {:keys [err-codes store]}]
  (fn [toasts]
    [:div.toast-container
     [:ul.toast-messages
      (for [[toast-id toast] (take 2 (sort-by key toasts))]
        ^{:key toast-id}
        [toast-message store err-codes toast-id toast])]]))
