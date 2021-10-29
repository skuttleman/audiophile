(ns com.ben-allred.audiophile.ui.core.components.core
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.components.protocols :as pcomp]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.core.forms.protocols :as pforms]
    [com.ben-allred.audiophile.ui.core.utils.dom :as dom]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]))

(def ^:private level->class
  {:error "is-danger"})

(defn alert [level body]
  [:div.message
   {:class [(level->class level)]}
   [:div.message-body
    body]])

(defmulti resource-error (fn [error-type _ _] error-type))

(defmethod resource-error :default
  [_ _ _]
  [:div.error "An error occurred. Please try again."])

(defmethod resource-error :http/timeout
  [_ _ _]
  [:div.error "The request timed out. Check your internet connection and refresh."])

(defn with-resource [*res component & args]
  (r/with-let [[*resource opts] (colls/force-sequential *res)
               _ (res/request! *resource opts)]
    (let [value @*resource
          error-type (if (some-> value meta :http/timeout?)
                       :http/timeout
                       (::error-type opts))]
      (case (res/status *resource)
        :success (into [component @*resource] args)
        :error [resource-error error-type value opts]
        [in/spinner {:size (:spinner/size opts)}]))))

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

(defn form [{:keys [buttons disabled *form on-submitted style] :as attrs} & fields]
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
         (cond-> {:on-submit (comp (fn [_]
                                     (when submittable?
                                       (cond-> (forms/attempt! *form)
                                         on-submitted on-submitted)))
                                   dom/prevent-default)}
           style (assoc :style style))]
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

(defn load! [player opts]
  (pcomp/load! player opts))

(defn id [player]
  (pcomp/id player))

(defn play-pause! [player]
  (pcomp/play-pause! player))

(defn destroy! [player]
  (pcomp/destroy! player))

(defn set-region!
  ([player]
   (set-region! player nil))
  ([player opts]
   (pcomp/set-region! player opts)))

(defn region [player]
  (pcomp/region player))

(defn ready? [player]
  (pcomp/ready? player))

(defn error? [player]
  (pcomp/error? player))

(defn playing? [player]
  (pcomp/playing? player))
