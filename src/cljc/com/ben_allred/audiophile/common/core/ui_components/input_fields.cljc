(ns com.ben-allred.audiophile.common.core.ui-components.input-fields
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.stubs.dom :as dom]
    [com.ben-allred.audiophile.common.core.stubs.reagent :as r]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]))

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
      (r/create-class
        {:component-did-update
         (fn [this _]
           (when-let [node @vnode]
             (when (and auto-focus? (not (:disabled (second (r/argv this)))))
               (vreset! vnode nil)
               (dom/focus node))))
         :reagent-render
         (fn [attrs & args]
           (into [component (cond-> (dissoc attrs :auto-focus?)
                              auto-focus? (assoc :ref ref :auto-focus true))]
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
                               (on-change (some-> attrs :value string/trim not-empty)))
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
                (merge (select-keys attrs #{:class :id :on-blur :ref :value :on-focus :auto-focus})))]])))))

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

(defn plain-button [{:keys [disabled] :as attrs} & content]
  (let [disabled #?(:cljs disabled :default true)]
    (-> attrs
        (maps/assoc-defaults :type :button)
        (assoc :disabled disabled)
        (cond-> disabled (update :class (fnil conj []) "is-disabled"))
        (->> (conj [:button.button]))
        (into content))))

(def ^{:arglists '([attrs true-display false-display])} button
  (with-auto-focus
    (with-id
      (fn [{:keys [disabled on-change value] :as attrs} true-display false-display]
        [form-field
         attrs
         [plain-button
          (-> attrs
              (select-keys #{:class :id :on-blur :ref})
              #?(:cljs (assoc :on-click #(on-change (not value)))))
          (if value true-display false-display)]]))))

(def ^{:arglists '([attrs])} file
  (with-auto-focus
    (with-id
      (fn [_]
        (let [file-input (volatile! nil)]
          (fn [{:keys [multi? on-change] :as attrs}]
            [form-field
             attrs
             [:div
              [:input {:ref      #(some->> % (vreset! file-input))
                       :type     :file
                       :multiple multi?
                       :style    {:display :none}
                       #?@(:cljs [:on-change (comp on-change
                                                   (fn [e]
                                                     (let [files (into #{} (some-> e .-target .-files))]
                                                       (doto (.-target e)
                                                         (aset "files" nil)
                                                         (aset "value" nil))
                                                       files)))])}]
              [plain-button (-> attrs
                                (select-keys #{:class :id :disabled :style :on-blur :ref :auto-focus})
                                #?(:cljs (assoc :on-click (comp (fn [_]
                                                                    (some-> @file-input .click))
                                                                  dom/prevent-default))))
               (:display attrs "Select file…")]]]))))))

(defn uploader [{:keys [multi? on-change resource] :as attrs}]
  [file (-> attrs
            (assoc :on-change (fn [file-set]
                                (-> resource
                                    (res/request! {:files file-set :multi? multi?})
                                    (v/then on-change))))
            (update :disabled #(or % (res/requesting? resource))))])

(defn openable [& _]
  (let [open? (r/atom false)
        ref (volatile! nil)
        listeners [(dom/add-listener dom/window :click (fn [e]
                                                         (if (->> (.-target e)
                                                                  (iterate #(some-> % .-parentNode))
                                                                  (take-while some?)
                                                                  (filter (partial = @ref))
                                                                  (empty?))
                                                           (do (reset! open? false)
                                                               (some-> @ref dom/blur))
                                                           (some-> @ref dom/focus))))
                   (dom/add-listener dom/window
                                     :keydown
                                     #(when (#{:key-codes/tab :key-codes/esc} (dom/event->key %))
                                        (reset! open? false))
                                     true)]]
    (r/create-class
      {:component-will-unmount
       (fn [_]
         (run! dom/remove-listener listeners))
       :reagent-render
       (fn [component & args]
         (let [attrs (-> {:on-toggle (fn [_]
                                       (swap! open? not))
                          :open?     @open?}
                         (update :ref (fn [ref-fn]
                                        (fn [node]
                                          (when node
                                            (vreset! ref node))
                                          (when ref-fn
                                            (ref-fn node)))))
                         (update :on-blur (fn [on-blur]
                                            (fn [e]
                                              (when-let [node @ref]
                                                (if @open?
                                                  (some-> node dom/focus)
                                                  (when on-blur
                                                    (on-blur e))))))))]
           (into [component attrs] args)))})))
