(ns com.ben-allred.audiophile.ui.core.components.core
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.components.protocols :as pcomp]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.core.forms.protocols :as pforms]
    [com.ben-allred.audiophile.ui.core.utils.dom :as dom]))

(def ^:private level->class
  {:error "is-danger"})

(defn alert [level body]
  [:div.message
   {:class [(level->class level)]}
   [:div.message-body
    body]])

(defn with-resource [[*resource opts] _component & _args]
  (res/request! *resource opts)
  (fn [_resource component & args]
    (case (res/status *resource)
      :success (into [component @*resource] args)
      :error [:div.error "an error occurred"]
      [in/spinner {:size (:spinner/size opts)}])))

(defn not-found [_]
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
         {:on-submit (comp (fn [_]
                             (when submittable?
                               (cond-> (forms/attempt! *form)
                                 on-submitted on-submitted)))
                           dom/prevent-default)}]
        (into fields)
        (conj (cond-> [:div.buttons
                       [in/plain-button
                        {:class    ["is-primary"]
                         :type     :submit
                         :disabled disabled}
                        (:submit/text attrs "Submit")]]
                (not ready?)
                (conj [:div {:style {:margin-bottom "8px"}} [in/spinner]])

                buttons
                (into buttons))))))

(defn create! [alert opts]
  (pcomp/create! alert opts))

(defn remove! [alert id]
  (pcomp/remove! alert id))

(defn remove-all! [alert]
  (pcomp/remove-all! alert))

(defn modal-opener [*modals title view]
  (fn [_]
    (create! *modals {:header [:h2.subtitle title] :body view})))
