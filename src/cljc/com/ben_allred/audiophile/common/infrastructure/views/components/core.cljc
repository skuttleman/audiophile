(ns com.ben-allred.audiophile.common.infrastructure.views.components.core
  (:require
    [com.ben-allred.audiophile.common.app.forms.core :as forms]
    [com.ben-allred.audiophile.common.app.resources.core :as res]
    [com.ben-allred.audiophile.common.app.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.stubs.dom :as dom]
    [com.ben-allred.audiophile.common.infrastructure.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.infrastructure.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.infrastructure.views.components.input-fields :as in]))

(def ^:private level->class
  {:error "is-danger"})

(defn alert [level body]
  [:div.message
   {:class [(level->class level)]}
   [:div.message-body
    body]])

(defn spinner [{:keys [size]}]
  [(keyword (str "div.loader." (name (or size :small))))])

(defn with-resource [[*resource opts] _component & _args]
  (res/request! *resource opts)
  (fn [_resource component & args]
    (let [status (res/status *resource)
          data @*resource]
      (case status
        :success (into [component data] args)
        :error [:div.error "an error occurred"]
        [spinner {:size (:spinner/size opts)}]))))

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

(defn form [{:keys [buttons disabled form on-submitted] :as attrs} & fields]
  (let [resource? (satisfies? pres/IResource form)
        ready? (if resource?
                 (res/ready? form)
                 true)
        disabled (or disabled
                     (not ready?)
                     (->> (forms/errors form)
                          maps/flatten
                          (filter (fn [[path]]
                                    (forms/touched? form path)))
                          seq))]
    (-> [:form.form.layout--stack-between
         {:on-submit (comp (fn [_]
                             (when resource?
                               (cond-> (res/request! form attrs)
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

(defn modal-opener [store title view]
  (fn [_]
    (ui-store/dispatch! store
                        (actions/modal! [:h2.subtitle title]
                                        view))))
