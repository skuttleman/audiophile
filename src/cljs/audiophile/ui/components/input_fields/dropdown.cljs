(ns audiophile.ui.components.input-fields.dropdown
  (:require
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.utils.dom :as dom]
    [clojure.set :as set]
    [audiophile.common.core.utils.colls :as colls]
    [reagent.core :as r]))

(defn option-list [{:keys [item-control on-change options value]}]
  (r/with-let [[selected unselected] (colls/split-on (comp (partial contains? value) first)
                                                     options)
               options (concat selected unselected)]
    [:ul.dropdown-items.lazy-list
     (for [[id display] options
           :let [selected? (contains? value id)]]
       ^{:key id}
       [:li.dropdown-item.pointer
        {:class    [(when selected? "is-active")]
         :on-click (comp (fn [_]
                           (-> (if (contains? value id)
                                 (disj value id)
                                 ((fnil conj #{}) value id))
                               on-change))
                         dom/stop-propagation!)}
        [item-control display]])]))

(defn button [{:keys [attrs->content selected] :as attrs}]
  (let [selected-count (count selected)
        content (if attrs->content
                  (attrs->content attrs)
                  (case selected-count
                    0 "Select…"
                    1 "1 Item Selected"
                    (str selected-count " Items Selected")))]
    [comp/plain-button
     (select-keys attrs #{:class :disabled :on-blur :on-click :ref})
     content
     [:span
      {:style {:margin-left "10px"}}
      [comp/icon (if (:open? attrs) :chevron-up :chevron-down)]]]))

(defn ^:private dropdown* [attrs]
  (let [{:keys [button-control loading? list-control on-search on-toggle open? options options-by-id value]
         :or   {list-control option-list button-control button}} attrs
        attrs (update attrs :on-change (fn [on-change]
                                         (fn [value]
                                           (when (:force-value? attrs)
                                             (on-toggle nil))
                                           (on-change value))))
        selected (seq (map options-by-id value))]
    [:div.dropdown
     {:class [(when open? "is-active")]}
     [:div.dropdown-trigger
      [button-control
       (-> attrs
           (set/rename-keys {:on-toggle :on-click})
           (cond->
             selected (assoc :selected selected)
             open? (update :class conj "is-focused")))]]
     (when open?
       [:div.dropdown-menu
        [:div.dropdown-content
         (when on-search
           [:div.dropdown-search
            [in/input {:on-change on-search}]])
         [:div.dropdown-body
          (cond
            loading?
            [comp/spinner]

            (seq options)
            [list-control attrs]

            :else
            [comp/alert :info "No results"])]]])]))

(defn ^:private openable-dropdown [attrs attrs']
  [dropdown* (merge attrs attrs')])

(defn dropdown [{:keys [options] :as attrs}]
  (let [options-by-id (or (:options-by-id attrs) (into {} options))]
    [comp/form-field
     attrs
     [in/openable openable-dropdown (assoc attrs :options-by-id options-by-id)]]))

(defn singleable [{:keys [force-value? value] :as attrs}]
  (let [value (if (nil? value) #{} #{value})]
    (-> attrs
        (assoc :value value)
        (update :on-change (fn [on-change]
                             (fn [values]
                               (let [value-next (first (remove value values))]
                                 (when (or (some? value-next) (not force-value?))
                                   (on-change value-next)))))))))
