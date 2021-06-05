(ns com.ben-allred.audiophile.ui.core.components.modal
  (:require
    [com.ben-allred.audiophile.ui.core.utils.dom :as dom]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.components.tiles :as tiles]))

(defn modal* [*modals _idx id _frame]
  (let [close! (comp (fn [_]
                       (comp/remove-one! *modals id))
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
       (fn [_*modals idx _id frame]
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

(defn modals [{:keys [*modals]}]
  (fn [modals]
    (let [active? (and (seq modals)
                       (or (seq (rest modals))
                           (not= :removing (:state (val (first modals))))))]
      (when (seq modals)
        [:div.modal-container
         {:class    [(when active? "is-active")]
          :on-click (when active?
                      (fn [_]
                        (comp/remove-all! *modals)))}
         [:div.modal-stack
          [:ul.modal-list
           (for [[idx [id frame]] (map-indexed vector (sort-by key modals))]
             ^{:key id}
             [modal* *modals idx id frame])]]]))))
