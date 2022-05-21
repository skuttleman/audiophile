(ns audiophile.ui.components.core
  (:require
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.utils.dom :as dom]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.forms.protocols :as pforms]))

(def ^:private level->class
  {:error "is-danger"})

(defn not-found [_ _]
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

(defn form [{:keys [buttons disabled *form on-submitted] :as attrs} & fields]
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
                                (cond-> (forms/attempt! *form)
                                  on-submitted on-submitted)))}
                (select-keys attrs #{:class :style}))]
        (into fields)
        (conj (cond-> [:div.buttons
                       [in/plain-button
                        {:class    ["is-primary" "submit"]
                         :type     :submit
                         :disabled disabled}
                        (:submit/text attrs "Submit")]]
                (not ready?)
                (conj [:div {:style {:margin-bottom "8px"}} [in/spinner]])

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
    [:div.panel-block
     body]]])

(defn with-resource [*resource & _]
  (let [[*resource opts] (colls/force-sequential *resource)]
    (when-not (res/requested? *resource)
      (res/request! *resource opts))
    (fn [_ comp & args]
      (let [status (res/status *resource)]
        (case status
          :success (into [comp @*resource] args)
          :error [:div.error "an error occurred"]
          [in/spinner {:size (:spinner/size opts)}])))))
