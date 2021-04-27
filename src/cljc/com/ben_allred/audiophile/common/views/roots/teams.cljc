(ns com.ben-allred.audiophile.common.views.roots.teams
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

(defn create* [_state *teams]
  (let [form (vres/create *teams (form/create {:team/type :COLLABORATIVE} validator))]
    (fn [_state _*teams]
      [:div
       [comp/form {:form form}
        [in/input (forms/with-attrs {:label       "Name"
                                     :auto-focus? true}
                                    form
                                    [:team/name])]]])))

(defmethod ig/init-key ::create [_ {:keys [teams]}]
  (fn [state]
    [create* state teams]))
