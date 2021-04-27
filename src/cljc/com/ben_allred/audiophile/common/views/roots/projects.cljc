(ns com.ben-allred.audiophile.common.views.roots.projects
  (:require
    [#?(:cljs    com.ben-allred.audiophile.ui.services.forms.standard
        :default com.ben-allred.audiophile.common.services.forms.noop) :as form]
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.common.services.resources.validated :as vres]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [com.ben-allred.audiophile.common.views.components.input-fields :as in]
    [com.ben-allred.audiophile.common.views.components.input-fields.dropdown :as dd]
    [com.ben-allred.vow.core :as v]
    [integrant.core :as ig]))

(def ^:private validator
  (constantly nil))

(defmulti team-name :team/type)

(defmethod team-name :PERSONAL
  [_]
  [:em "MY PERSONAL STUFF"])

(defmethod team-name :COLLABORATIVE
  [{:team/keys [name]}]
  [:em name])

(def ^:private personal?
  (comp #{:PERSONAL} :team/type))

(defn create* [teams _state *projects]
  (let [form (vres/create *projects (form/create nil validator))
        options (->> teams
                     (remove personal?)
                     (concat (filter personal? teams))
                     (map (juxt :team/id identity)))
        options-by-id (into {} options)]
    (fn [_teams _state _*projects]
      [:div
       [comp/form {:form form}
        [dd/dropdown (-> {:options       options
                          :options-by-id options-by-id
                          :item-control  team-name
                          :force-value?  true
                          :label         "Team"}
                         (forms/with-attrs form [:project/team-id])
                         dd/singleable)]
        [in/input (forms/with-attrs {:label       "Name"
                                     :auto-focus? true}
                                    form
                                    [:project/name])]]])))

(defmethod ig/init-key ::create [_ {:keys [projects teams]}]
  (fn [state]
    [comp/with-resource [teams] create* state projects]))
