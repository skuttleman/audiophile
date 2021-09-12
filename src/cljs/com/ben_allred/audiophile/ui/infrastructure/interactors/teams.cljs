(ns com.ben-allred.audiophile.ui.infrastructure.interactors.teams
  (:require
    [com.ben-allred.audiophile.common.domain.validations.core :as val]
    [com.ben-allred.audiophile.common.domain.validations.specs :as specs]
    [com.ben-allred.audiophile.ui.api.forms.standard :as form]
    [com.ben-allred.audiophile.ui.api.forms.submittable :as form.sub]
    [com.ben-allred.audiophile.ui.api.views.protocols :as vp]
    [com.ben-allred.vow.core :as v]))

(def ^:private validator
  (val/validator {:spec specs/team:create}))

(deftype TeamsPageInteractor [*teams ->cb]
  vp/ITeamsViewInteractor
  (team-form [_]
    (form.sub/create *teams (form/create {:team/type :COLLABORATIVE} validator)))
  (on-team-created [_ cb]
    (fn [vow]
      (v/peek vow (->cb cb) nil))))

(defn interactor [{:keys [*teams ->cb]}]
  (->TeamsPageInteractor *teams ->cb))
