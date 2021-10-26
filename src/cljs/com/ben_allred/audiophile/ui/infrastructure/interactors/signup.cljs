(ns com.ben-allred.audiophile.ui.infrastructure.interactors.signup
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.domain.validations.core :as val]
    [com.ben-allred.audiophile.common.domain.validations.specs :as specs]
    [com.ben-allred.audiophile.ui.api.forms.standard :as form]
    [com.ben-allred.audiophile.ui.api.forms.submittable :as form.sub]
    [com.ben-allred.audiophile.ui.api.views.protocols :as vp]
    [com.ben-allred.audiophile.ui.core.utils.dom :as dom]
    [com.ben-allred.vow.core :as v]))

(def ^:private validator
  (val/validator {:spec specs/user:create}))

(deftype SignupViewInteractor [*users *handles *mobiles cache]
  vp/ISignupViewInteractor
  (signup-form [_]
    (form.sub/create *users (form/create nil validator)))
  (on-user-created [_ cb]
    (fn [vow]
      (v/peek vow cb nil)))

  vp/IAsyncFieldValidator
  (field-resource [_ path]
    (case path
      [:user/handle] *handles
      [:user/mobile-number] *mobiles))
  (on-blur [_ path]
    (let [*resource (case path
                      [:user/handle] *handles
                      [:user/mobile-number] *mobiles)
          field (first path)]
      (fn [event]
        (let [value (dom/target-value event)
              prev-val (get @cache path)]
          (when-not (or (get-in (validator (assoc-in {} path value)) path)
                        (= value prev-val))
            (swap! cache assoc path value)
            (res/request! *resource
                          {:field/entity (namespace field)
                           :field/name   (name field)
                           :field/value  value})))))))

(defn interactor [{:keys [*handles *mobiles *users]}]
  (->SignupViewInteractor *users *handles *mobiles (atom {})))
