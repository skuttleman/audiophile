(ns audiophile.ui.components.modals
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.utils.dom :as dom]
    [audiophile.ui.store.actions :as act]
    [reagent.core :as r]))

(defmulti body (fn [k _ _]
                 k))

(defn modal* [{:keys [store] :as sys} _idx id _frame]
  (letfn [(close! [_]
            (store/dispatch! store (act/modal:remove! id)))]

    (let [stop-and-close! (comp close! dom/stop-propagation!)
          listener (dom/add-listener js/window
                                     :keydown
                                     #(when (#{:key-codes/esc} (dom/event->key %))
                                        (stop-and-close! %))
                                     true)]
      (r/create-class
        {:component-will-unmount
         (fn [_]
           (dom/remove-listener listener))
         :reagent-render
         (fn [_sys idx _id frame]
           (let [inset (str (* 8 idx) "px")]
             [:li.modal-item
              {:class    [(case (:state frame)
                            :init "adding"
                            :removing "removing"
                            nil)]
               :style    {:padding-left inset
                          :padding-top  inset}
               :on-click dom/stop-propagation!}
              [comp/tile
               [:div.layout--space-between
                [:div (:header frame)]
                [in/plain-button
                 {:class    ["is-white" "is-light"]
                  :on-click stop-and-close!}
                 [comp/icon :times]]]
               (when-let [[k attrs] (:body frame)]
                 [:div [body k sys (assoc attrs :close! close!)]])]]))}))))

(defn root [{:keys [store] :as sys} modals]
  (let [active? (and (seq modals)
                     (or (seq (rest modals))
                         (not= :removing (:state (val (first modals))))))]
    (when (seq modals)
      [:div.modal-container
       {:class    [(when active? "is-active")]
        :on-click (when active?
                    (fn [_]
                      (store/dispatch! store act/modal:remove-all!)))}
       [:div.modal-stack
        [:ul.modal-list
         (for [[idx [id frame]] (map-indexed vector (sort-by key modals))]
           ^{:key id}
           [modal* sys idx id frame])]]])))
