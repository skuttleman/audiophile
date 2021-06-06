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

(defn spinner
  ([]
   (spinner nil))
  ([{:keys [size]}]
   [(keyword (str "div.loader." (name (or size :small))))]))

(defn with-resource [[*resource opts] _component & _args]
  (res/request! *resource opts)
  (fn [_resource component & args]
    (case (res/status *resource)
      :success (into [component @*resource] args)
      :error [:div.error "an error occurred"]
      [spinner {:size (:spinner/size opts)}])))

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
                (conj [:div {:style {:margin-bottom "8px"}} [spinner nil]])

                buttons
                (into buttons))))))

(defn create! [alert level body]
  (pcomp/create! alert level body))

(defn remove! [alert id]
  (pcomp/remove! alert id))

(defn modal!
  ([modals body]
   (modal! modals nil body))
  ([modals header body]
   (modal! modals header body nil))
  ([modals header body buttons]
   (pcomp/modal! modals header body buttons)))

(defn remove-one! [modals id]
  (pcomp/remove-one! modals id))

(defn remove-all! [modals]
  (pcomp/remove-all! modals))

(defn modal-opener [*modals title view]
  (fn [_]
    (modal! *modals [:h2.subtitle title] view)))
