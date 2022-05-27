(ns audiophile.ui.components.core
  (:require
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.forms.protocols :as pforms]
    [audiophile.ui.utils.dom :as dom]))

(def ^:private level->class
  {:error "is-danger"})

(defn form-field [{:keys [attempted? errors form-field-class id label label-small?]} & body]
  (let [errors (seq (remove nil? errors))
        show-errors? (and errors attempted?)]
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

(defn plain-button [{:keys [disabled] :as attrs} & content]
  (-> attrs
      (maps/assoc-defaults :type :button)
      (assoc :disabled disabled)
      (cond-> disabled (update :class (fnil conj []) "is-disabled"))
      (->> (conj [:button.button]))
      (into content)))

(defn spinner
  ([]
   (spinner nil))
  ([{:keys [size]}]
   [(keyword (str "div.loader." (name (or size :small))))]))

(defn within-ref? [target node]
  (->> target
       (iterate #(some-> % .-parentNode))
       (take-while some?)
       (filter #{node})
       seq
       boolean))

(defn not-found [_]
  [:div {:style {:display         :flex
                 :align-items     :center
                 :justify-content :center}}
   [:img {:src "https://i.imgur.com/HYpqZvg.jpg"}]])

(defn alert [level body]
  [:div.message
   {:class [(level->class level)]}
   [:div.message-body
    body]])

(defn icon
  ([icon-class]
   (icon {} icon-class))
  ([attrs icon-class]
   [:i.fas (update attrs :class conj (str "fa-" (name icon-class)))]))

(defn form [{:keys [buttons disabled *form] :as attrs} & fields]
  (let [submittable? (satisfies? pforms/IAttempt *form)
        ready? (if submittable?
                 (not (forms/attempting? *form))
                 true)
        disabled (or disabled
                     (not ready?)
                     (and (forms/errors *form)
                          (or (not submittable?)
                              (and submittable?
                                   (forms/attempted? *form)))))]
    (-> [:form.form.layout--stack-between
         (merge {:on-submit (fn [e]
                              (dom/prevent-default! e)
                              (when submittable?
                                (forms/attempt! *form)))}
                (select-keys attrs #{:class :style}))]
        (into fields)
        (conj (cond-> [:div.buttons
                       [plain-button
                        {:class    ["is-primary" "submit"]
                         :type     :submit
                         :disabled disabled}
                        (:submit/text attrs "Submit")]]
                (not ready?)
                (conj [:div {:style {:margin-bottom "8px"}} [spinner]])

                buttons
                (into buttons))))))

(defn tile [heading body & tabs]
  [:div.tile
   [:div.panel {:style {:min-width        "400px"
                        :background-color "#fcfcfc"}}
    (when heading
      [:div.panel-heading
       heading])
    (when (seq tabs)
      (into [:div.panel-tabs
             {:style {:padding         "8px"
                      :justify-content :flex-start}}]
            tabs))
    [:div.panel-block.block
     body]]])

(defn with-resource [*resource comp]
  (let [[*resource opts] (colls/force-sequential *resource)
        comp (colls/force-sequential comp)]
    (when-not (res/requested? *resource)
      (res/request! *resource opts))
    (fn [_ _]
      (let [status (res/status *resource)]
        (case status
          :success (conj comp @*resource)
          :error [:div.error [alert :error "An error occurred."]]
          [spinner {:size (:spinner/size opts)}])))))
