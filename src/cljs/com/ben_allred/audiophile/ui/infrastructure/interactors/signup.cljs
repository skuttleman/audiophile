(ns com.ben-allred.audiophile.ui.infrastructure.interactors.signup
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.domain.validations.core :as val]
    [com.ben-allred.audiophile.common.domain.validations.specs :as specs]
    [com.ben-allred.audiophile.ui.api.forms.standard :as form]
    [com.ben-allred.audiophile.ui.api.forms.submittable :as form.sub]
    [com.ben-allred.audiophile.ui.api.views.protocols :as vp]
    [com.ben-allred.vow.core :as v]))

(def ^:private validator
  (val/validator {:spec specs/user:create}))

(deftype SignupViewInteractor [*users]
  vp/ISignupViewInteractor
  (signup-form [_]
    (form.sub/create *users (form/create nil validator)))
  (on-user-created [_ cb]
    (fn [vow]
      (v/peek vow cb nil))))

(defn interactor [{:keys [*users]}]
  (->SignupViewInteractor *users))
