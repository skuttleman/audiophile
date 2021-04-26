(ns com.ben-allred.audiophile.common.views.roots.projects
  (:require
    [#?(:cljs    com.ben-allred.audiophile.ui.services.forms.standard
        :default com.ben-allred.audiophile.common.services.forms.noop) :as form]
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.common.services.resources.validated :as vres]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [com.ben-allred.audiophile.common.views.components.input-fields :as in]
    [integrant.core :as ig]))

(def ^:private validator
  (constantly nil))

(defn create* [_data _state projects]
  (let [form (vres/create projects (form/create nil validator))]
    (fn [data state _projects]
      [:div
       [log/pprint data]
       [comp/form {:form form}
        [in/input (forms/with-attrs {:label       "Name"
                                     :auto-focus? true}
                                    form
                                    [:project/name])]]])))

(defmethod ig/init-key ::create [_ {:keys [projects teams]}]
  (fn [state]
    [comp/with-resource [teams] create* state projects]))
