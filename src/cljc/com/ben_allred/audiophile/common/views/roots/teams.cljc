(ns com.ben-allred.audiophile.common.views.roots.teams
  (:require
    [#?(:cljs    com.ben-allred.audiophile.ui.services.forms.standard
        :default com.ben-allred.audiophile.common.services.forms.noop) :as form]
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.common.services.resources.core :as res]
    [com.ben-allred.audiophile.common.services.resources.validated :as vres]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [com.ben-allred.audiophile.common.views.components.input-fields :as in]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]
    [integrant.core :as ig]))

(def ^:private validator
  (constantly nil))

(defmethod ig/init-key ::list [_ _]
  (fn [teams _state]
    [:div
     [:p [:strong "Your teams"]]
     (if (seq teams)
       [:ul
        (for [{team-name :team/name :team/keys [id type]} teams]
          ^{:key id}
          [:li "• " team-name (str " - " type)])]
       [:p "You don't have any teams. Why not create one?"])]))

(defn create* [*teams _cb]
  (let [form (vres/create *teams (form/create {:team/type :COLLABORATIVE} validator))]
    (fn [_*teams cb]
      [:div
       [comp/form {:form         form
                   :on-submitted (fn [vow]
                                   (v/peek vow cb nil))}
        [in/input (forms/with-attrs {:label       "Name"
                                     :auto-focus? true}
                                    form
                                    [:team/name])]]])))

(defmethod ig/init-key ::create [_ {:keys [*all-teams *teams]}]
  (fn [cb]
    [create* *teams (fn [result]
                     (res/request! *all-teams)
                     (when cb
                       (cb result)))]))
