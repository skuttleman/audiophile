(ns com.ben-allred.audiophile.common.infrastructure.views.components.modal
  (:require
    [com.ben-allred.audiophile.common.core.stubs.dom :as dom]
    [com.ben-allred.audiophile.common.infrastructure.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.infrastructure.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.views.components.core :as comp]
    [com.ben-allred.audiophile.common.infrastructure.views.components.input-fields :as in]
    [com.ben-allred.audiophile.common.infrastructure.views.components.tiles :as tiles]
    [com.ben-allred.audiophile.common.core.stubs.reagent :as r]))

(defn modal* [store _idx id _frame]
  (let [close! (comp (fn [_]
                       (ui-store/dispatch! store (actions/remove-modal! id)))
                     dom/stop-propagation)
        listener (dom/add-listener dom/window
                                   :keydown
                                   #(when (#{:key-codes/esc} (dom/event->key %))
                                      (close! %))
                                   true)]
    (r/create-class
      {:component-will-unmount
       (fn [_]
         (dom/remove-listener listener))
       :reagent-render
       (fn [_store idx _id frame]
         (let [inset (str (* 8 idx) "px")]
           [:li.modal-item
            {:class    [(case (:state frame)
                          :init "adding"
                          :removing "removing"
                          nil)]
             :style    {:padding-left inset
                        :padding-top  inset}
             :on-click dom/stop-propagation}
            [tiles/tile
             [:div.layout--space-between
              [:div (:header frame)]
              [in/plain-button
               {:class    ["is-white" "is-light"]
                :on-click close!}
               [comp/icon :times]]]
             [:div (some-> (:body frame) (conj close!))]]]))})))

(defn modals [{:keys [store]}]
  (fn [modals]
    (let [active? (and (seq modals)
                       (or (seq (rest modals))
                           (not= :removing (:state (val (first modals))))))]
      (when (seq modals)
        [:div.modal-container
         {:class    [(when active? "is-active")]
          :on-click (when active?
                      (fn [_]
                        (ui-store/dispatch! store actions/remove-modal-all!)))}
         [:div.modal-stack
          [:ul.modal-list
           (for [[idx [id frame]] (map-indexed vector (sort-by key modals))]
             ^{:key id}
             [modal* store idx id frame])]]]))))
