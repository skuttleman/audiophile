(ns audiophile.backend.api.handlers.search
  (:require
    [audiophile.backend.api.validations.selectors :as selectors]
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.common.core.utils.logger :as log]))

(defn search [{:keys [interactor]}]
  (fn [data]
    {:in-use? (int/exists? interactor data)}))

(defmethod selectors/select [:get :api/search]
  [_ request]
  (let [params (get-in request [:nav/route :params])]
    {:search/field (keyword (:field/entity params)
                            (:field/name params))
     :search/value (:field/value params)
     :token/aud    (get-in request [:auth/user :jwt/aud])}))
