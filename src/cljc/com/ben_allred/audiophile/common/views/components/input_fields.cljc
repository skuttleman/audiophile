(ns com.ben-allred.audiophile.common.views.components.input-fields
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.utils.dom :as dom]
    [com.ben-allred.audiophile.common.utils.stubs :as st]))

(defn form-field [{:keys [attempted? errors form-field-class id label label-small? visited?]} & body]
  (let [errors (seq (remove nil? errors))
        show-errors? (and errors (or visited? attempted?))]
    [:div.form-field
     {:class (into [(when show-errors? "errors")] form-field-class)}
     [:<>
      (when label
        [:label.label
         (cond-> {:html-for id}
           label-small? (assoc :style {:font-weight :normal
                                       :font-size   "0.8em"}))
         label])
      (into [:div.form-field-control] body)]
     (when show-errors?
       [:ul.error-list
        (for [error errors]
          [:li.error
           {:key error}
           error])])]))

(defn ^:private with-auto-focus [component]
  (fn [{:keys [auto-focus?]} & _]
    (let [vnode (volatile! nil)
          ref (fn [node] (some->> node (vreset! vnode)))]
      (st/create-class
        {:component-did-update
         (fn [this _]
           (when-let [node @vnode]
             (when (and auto-focus? (not (:disabled (second (st/argv this)))))
               (vreset! vnode nil)
               (dom/focus node))))
         :reagent-render
         (fn [attrs & args]
           (into [component (cond-> (dissoc attrs :auto-focus?)
                              auto-focus? (assoc :ref ref))]
                 args))}))))

(defn ^:private with-id [component]
  (fn [_attrs & _args]
    (let [id (gensym "form-field")]
      (fn [attrs & args]
        (into [component (assoc attrs :id id)] args)))))

(defn ^:private with-trim-blur [component]
  (fn [attrs & args]
    (-> attrs
        (update :on-blur (fn [on-blur]
                           (fn [e]
                             (when-let [on-change (:on-change attrs)]
                               (on-change (not-empty (string/trim (:value attrs)))))
                             (when on-blur
                               (on-blur e)))))
        (->> (conj [component]))
        (into args))))

(def ^{:arglists '([attrs options])} select
  (with-auto-focus
    (with-id
      (fn [{:keys [disabled on-change value] :as attrs} options]
        (let [option-values (set (map first options))
              value (if (contains? option-values value)
                      value
                      ::empty)]
          [form-field
           attrs
           [:select.select
            (-> {:value    (str value)
                 :disabled #?(:clj true :cljs disabled)
                 #?@(:cljs [:on-change (comp on-change
                                             (into {} (map (juxt str identity) option-values))
                                             dom/target-value)])}
                (merge (select-keys attrs #{:class :id :on-blur :ref})))
            (for [[option label attrs] (cond->> options
                                                (= ::empty value) (cons [::empty
                                                                         (str "Choose" #?(:clj "..." :cljs "…"))
                                                                         {:disabled true}]))
                  :let [str-option (str option)]]
              [:option
               (assoc attrs :value str-option :key str-option #?@(:clj [:selected (= option value)]))
               label])]])))))

(def ^{:arglists '([attrs])} textarea
  (with-auto-focus
    (with-id
      (with-trim-blur
        (fn [{:keys [disabled on-change value] :as attrs}]
          [form-field
           attrs
           [:textarea.textarea
            (-> {:value    value
                 :disabled #?(:clj true :cljs disabled)
                 #?@(:cljs [:on-change (comp on-change dom/target-value)])}
                (merge (select-keys attrs #{:class :id :on-blur :ref})))
            #?(:clj value)]])))))

(def ^{:arglists '([attrs])} input
  (with-auto-focus
    (with-id
      (with-trim-blur
        (fn [{:keys [disabled on-change type] :as attrs}]
          [form-field
           attrs
           [:input.input
            (-> {:type     (or type :text)
                 :disabled #?(:clj true :cljs disabled)
                 #?@(:cljs [:on-change (comp on-change dom/target-value)])}
                (merge (select-keys attrs #{:class :id :on-blur :ref :value})))]])))))

(def ^{:arglists '([attrs])} checkbox
  (with-auto-focus
    (with-id
      (fn [{:keys [disabled on-change value] :as attrs}]
        [form-field
         attrs
         [:input.checkbox
          (-> {:checked  (boolean value)
               :type     :checkbox
               :disabled #?(:clj true :cljs disabled)
               #?@(:cljs [:on-change #(on-change (not value))])}
              (merge (select-keys attrs #{:class :id :on-blur :ref})))]]))))

(def ^{:arglists '([attrs])} button
  (with-auto-focus
    (with-id
      (fn [{:keys [disabled on-change value] :as attrs} true-display false-display]
        [form-field
         attrs
         [:button.button
          (-> {:type     :button
               :disabled #?(:clj true :cljs disabled)
               #?@(:cljs [:on-click #(on-change (not value))])}
              (merge (select-keys attrs #{:class :id :on-blur :ref})))
          (if value true-display false-display)]]))))
