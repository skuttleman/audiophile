(ns com.ben-allred.audiophile.ui.infrastructure.components.core
  (:require
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.resources.core :as res]
    [com.ben-allred.audiophile.ui.infrastructure.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.infrastructure.dom :as dom]
    [com.ben-allred.audiophile.ui.infrastructure.forms.core :as forms]
    [com.ben-allred.audiophile.ui.infrastructure.forms.protocols :as pforms]))

(defn not-found [_ _]
  [:div {:style {:display         :flex
                 :align-items     :center
                 :justify-content :center}}
   [:img {:src "https://i.imgur.com/HYpqZvg.jpg"}]])

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
    (res/request! *resource opts)
    (fn [_ comp & args]
      (let [status (res/status *resource)]
        (case status
          :success (into [comp @*resource] args)
          :error [:div.error "an error occurred"]
          [in/spinner {:size (:spinner/size opts)}])))))
