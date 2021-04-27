(ns com.ben-allred.audiophile.common.views.components.core
  (:require
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.common.services.resources.core :as res]
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.services.stubs.dom :as dom]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.vow.core :as v]))

(def ^:private level->class
  {:error "is-danger"})

(defn alert [level body]
  [:div.message
   {:class [(level->class level)]}
   [:div.message-body
    body]])

(defn spinner [{:keys [size]}]
  [(keyword (str "div.loader." (name (or size :small))))])

(defn with-resource [[resource opts] _component & _args]
  (res/request! resource opts)
  (fn [_resource component & args]
    (let [status (res/status resource)
          data @resource]
      (case status
        :success (into [component data] args)
        :error [:div.error "an error occurred"]
        [spinner {:size (:spinner/size opts)}]))))

(defn not-found [_]
  [:div "not found"])

(defn icon
  ([icon-class]
   (icon {} icon-class))
  ([attrs icon-class]
   [:i.fas (update attrs :class conj (str "fa-" (name icon-class)))]))

(defn form [{:keys [buttons disabled form on-submitted] :as attrs} & fields]
  (let [ready? (when (satisfies? pres/IResource form)
                 (res/ready? form))
        disabled (or disabled
                     (not ready?)
                     (->> (forms/errors form)
                          maps/flatten
                          (filter (fn [[path]]
                                    (forms/visited? form path)))
                          seq))]
    (-> [:form.form.layout--stack-between
         {:on-submit (comp (fn [_]
                             (when (satisfies? pres/IResource form)
                               (cond-> (res/request! form attrs)
                                 on-submitted on-submitted)))
                           dom/prevent-default)}]
        (into fields)
        (conj (cond-> [:div.buttons
                       [:button.button.is-primary
                        {:type     :submit
                         :disabled disabled}
                        (:submit/text attrs "Submit")]]
                buttons
                (into buttons)

                (not ready?)
                (conj [:div {:style {:margin-bottom "8px"}} [spinner nil]]))))))
