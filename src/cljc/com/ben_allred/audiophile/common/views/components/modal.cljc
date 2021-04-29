(ns com.ben-allred.audiophile.common.views.components.modal
  (:require
    [com.ben-allred.audiophile.common.services.stubs.dom :as dom]
    [com.ben-allred.audiophile.common.services.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defn modal [store idx id frame]
  (let [inset (str (* 8 idx) "px")]
    [:li.modal-item
     {:class    [(case (:state frame)
                   :init "adding"
                   :removing "removing"
                   nil)]
      :on-click (comp (fn [_]
                        (ui-store/dispatch! store (actions/remove-modal! id)))
                      dom/stop-propagation)
      :style    {:padding-left inset
                 :padding-top  inset}}
     [log/pprint frame]]))

(defmethod ig/init-key ::modals [_ {:keys [store]}]
  (fn [modals]
    (let [active? (and (seq modals)
                     (or (seq (rest modals))
                         (not= :removing (:state (val (first modals))))))]
      [:div.modal-container
       {:class    [(when active? "is-active")]
        :on-click (when active?
                    (fn [_]
                      (ui-store/dispatch! store actions/remove-modal-all!)))}
       [:div.modal-stack
        (when (seq modals)
          [:ul.modal-list
           (for [[idx [id frame]] (map-indexed vector (sort-by key modals))]
             ^{:key id}
             [modal store idx id frame])])]])))
