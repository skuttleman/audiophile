(ns audiophile.ui.components.input-fields
  (:require
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.utils.dom :as dom]
    [clojure.string :as string]
    [com.ben-allred.vow.core :as v]
    [reagent.core :as r]))

(defn progress-bar [{:keys [current height status total]}]
  (let [height (str (or height 6) "px")
        complete? (contains? #{:success :error} status)]
    [:div {:style {:height height}}
     (when-let [percent (cond
                          complete? 1
                          total (min (/ current total) 0.99)
                          current 0)]
       [:div.progress-bar
        {:style {:height height}}
        [:div.progress-amount
         {:class [(cond
                    (zero? percent) "unstarted"
                    complete? "complete")]
          :style {:background-color (case status
                                      :error "red"
                                      :success "green"
                                      "blue")
                  :height           height
                  :width            (str (* 100 percent) "%")}}]])]))

(defn ^:private with-auto-focus [component]
  (fn [{:keys [auto-focus?]} & _]
    (let [vnode (volatile! nil)
          ref (fn [node] (some->> node (vreset! vnode)))]
      (r/create-class
        {:component-did-mount
         (fn [this _]
           (when-let [node @vnode]
             (when (and auto-focus? (not (:disabled (second (r/argv this)))))
               (vreset! vnode nil)
               (.focus node))))
         :reagent-render
         (fn [attrs & args]
           (into [component (cond-> (dissoc attrs :auto-focus?)
                              auto-focus? (assoc :ref ref :auto-focus true))]
                 args))}))))

(defn ^:private with-id [component]
  (fn [attrs & args]
    (r/with-let [id (gensym "form-field")]
      (into [component (assoc attrs :id id)] args))))

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
          [comp/form-field
           attrs
           [:select.select
            (-> {:value     (str value)
                 :disabled  disabled
                 :on-change (comp on-change
                                  (into {} (map (juxt str identity) option-values))
                                  dom/target-value)}
                (merge (select-keys attrs #{:class :id :on-blur :ref})))
            (for [[option label attrs] (cond->> options
                                                (= ::empty value) (cons [::empty
                                                                         "Choose…"
                                                                         {:disabled true}]))
                  :let [str-option (str option)]]
              [:option
               (assoc attrs :value str-option :key str-option :selected (= option value))
               label])]])))))

(def ^{:arglists '([attrs])} textarea
  (with-auto-focus
    (with-id
      (with-trim-blur
        (fn [{:keys [disabled on-change value] :as attrs}]
          [comp/form-field
           attrs
           [:textarea.textarea
            (-> {:value     value
                 :disabled  disabled
                 :on-change (comp on-change dom/target-value)}
                (merge (select-keys attrs #{:class :id :on-blur :ref})))]])))))

(def ^{:arglists '([attrs])} input
  (with-auto-focus
    (with-id
      (with-trim-blur
        (fn [{:keys [disabled on-change type] :as attrs}]
          [comp/form-field
           attrs
           [:input.input
            (-> {:type      (or type :text)
                 :disabled  disabled
                 :on-change (comp on-change dom/target-value)}
                (merge (select-keys attrs #{:class :id :on-blur :ref :value :on-focus :auto-focus})))]])))))

(def ^{:arglists '([attrs])} checkbox
  (with-auto-focus
    (with-id
      (fn [{:keys [disabled on-change value] :as attrs}]
        [comp/form-field
         attrs
         [:input.checkbox
          (-> {:checked   (boolean value)
               :type      :checkbox
               :disabled  disabled
               :on-change #(on-change (not value))}
              (merge (select-keys attrs #{:class :id :on-blur :ref})))]]))))

(def ^{:arglists '([attrs true-display false-display])} button
  (with-auto-focus
    (with-id
      (fn [{:keys [on-change value] :as attrs} true-display false-display]
        [comp/form-field
         attrs
         [comp/plain-button
          (-> attrs
              (select-keys #{:class :id :on-blur :ref})
              (assoc :on-click #(on-change (not value))))
          (if value true-display false-display)]]))))

(def ^{:arglists '([attrs])} file
  (with-auto-focus
    (with-id
      (fn [{:keys [multi? on-change progress] :as attrs}]
        (r/with-let [file-input (volatile! nil)]
          [comp/form-field
           (cond-> attrs
             (not (:attempted? attrs)) (dissoc :errors))
           [:div
            [:input {:ref       #(some->> % (vreset! file-input))
                     :type      :file
                     :accept    ".mp3,.wav,.ogg"
                     :multiple  multi?
                     :style     {:display :none}
                     :on-change (comp on-change
                                      (fn [e]
                                        (let [files (into #{} (some-> e .-target .-files))]
                                          (doto (.-target e)
                                            (aset "files" nil)
                                            (aset "value" nil))
                                          files)))}]
            [comp/plain-button
             (-> attrs
                 (select-keys #{:class :id :disabled :style :on-blur :ref :auto-focus})
                 (assoc :on-click (comp (fn [_]
                                          (dom/click! @file-input))
                                        dom/prevent-default!)))
             (:display attrs "Select file…")
             (when (:disabled attrs)
               [:div {:style {:margin-left "8px"}} [comp/spinner]])]
            (when progress
              [progress-bar progress])]])))))

(defn ^:private on-progress [progress {:progress/keys [current total status]}]
  (let [result (maps/->m current status total)]
    (swap! progress (fn [{:keys [status] :as data}]
                      (if status data result)))))

(defn uploader [{:keys [multi? on-change *resource] :or {on-change identity} :as attrs}]
  (r/with-let [progress (r/atom nil)
               on-progress (partial on-progress progress)]
    [file (-> attrs
              (assoc :progress @progress
                     :on-change (fn [file-set]
                                  (reset! progress {:current 0 :total nil})
                                  (-> *resource
                                      (res/request! {:files       file-set
                                                     :multi?      multi?
                                                     :on-progress on-progress})
                                      (v/then on-change))))
              (update :disabled #(or % (res/requesting? *resource))))]))

(defn ^:private opener-click [ref open?]
  (fn [e]
    (if (comp/within-ref? (.-target e) @ref)
      (some-> @ref dom/focus!)
      (do (reset! open? false)
          (some-> @ref dom/blur!)))))

(defn ^:private opener-keydown [open?]
  (fn [e]
    (case (dom/event->key e)
      (:key-codes/tab :key-codes/esc) (reset! open? false)
      nil)))

(defn openable [component & args]
  (r/with-let [open? (r/atom false)
               ref (volatile! nil)
               listeners [(dom/add-listener js/window
                                            :click
                                            (opener-click ref open?))
                          (dom/add-listener js/window
                                            :keydown
                                            (opener-keydown open?)
                                            true)]
               on-toggle (fn [_]
                           (swap! open? not))
               ref-fn (fn [node]
                        (some->> node (vreset! ref)))
               on-blur (fn [_]
                         (when-let [node @ref]
                           (when @open?
                             (some-> node dom/focus!))))]
    (into [component {:open?     @open?
                      :on-toggle on-toggle
                      :ref       ref-fn
                      :on-blur   on-blur}]
          args)
    (finally (run! dom/remove-listener listeners))))
