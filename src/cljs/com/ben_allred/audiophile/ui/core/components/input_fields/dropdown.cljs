(ns com.ben-allred.audiophile.ui.core.components.input-fields.dropdown
  (:require
    [clojure.set :as set]
    [com.ben-allred.audiophile.ui.core.utils.dom :as dom]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn option-list [{:keys [value options]}]
  (let [options (concat (filter (comp (partial contains? value) first) options)
                        (remove (comp (partial contains? value) first) options))]
    (fn [{:keys [item-control on-change value]}]
      [:ul.dropdown-items.lazy-list
       (for [[id display] options
             :let [selected? (contains? value id)]]
         ^{:key id}
         [:li.dropdown-item.pointer
          {:class    [(when selected? "is-active")]
           :on-click (fn [e]
                       (dom/stop-propagation e)
                       (-> (if (contains? value id)
                             (disj value id)
                             ((fnil conj #{}) value id))
                           on-change))}
          [item-control display]])])))

(defn button [{:keys [attrs->content selected] :as attrs}]
  (let [selected-count (count selected)
        content (if attrs->content
                  (attrs->content attrs)
                  (case selected-count
                    0 "Select…"
                    1 "1 Item Selected"
                    (str selected-count " Items Selected")))]
    [in/plain-button
     (select-keys attrs #{:class :disabled :on-blur :on-click :ref})
     content
     [:span
      {:style {:margin-left "10px"}}
      [comp/icon (if (:open? attrs) :chevron-up :chevron-down)]]]))

(defn ^:private dropdown* [attrs' attrs'']
  (let [{:keys [button-control loading? list-control on-search on-toggle open? options options-by-id value]
         :or   {list-control option-list button-control button}
         :as   attrs} (merge attrs' attrs'')
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
            [comp/spinner nil]

            (seq options)
            [list-control attrs]

            :else
            [comp/alert :info "No results"])]]])]))

(defn dropdown [{:keys [options] :as attrs}]
  (let [options-by-id (or (:options-by-id attrs) (into {} options))]
    [in/form-field
     attrs
     [in/openable dropdown* (assoc attrs :options-by-id options-by-id)]]))

(defn singleable [{:keys [force-value? value] :as attrs}]
  (let [value (if (nil? value) #{} #{value})]
    (-> attrs
        (assoc :value value)
        (update :on-change (fn [on-change]
                             (fn [values]
                               (let [value-next (first (remove value values))]
                                 (when (or (some? value-next) (not force-value?))
                                   (on-change value-next)))))))))
