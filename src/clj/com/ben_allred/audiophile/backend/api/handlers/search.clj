(ns com.ben-allred.audiophile.backend.api.handlers.search
  (:require
    [com.ben-allred.audiophile.backend.api.validations.selectors :as selectors]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn search [{:keys [interactor]}]
  (fn [data]
    {:in-use? (int/exists? interactor data)}))

(defmethod selectors/select [:get :api/search]
  [_ request]
  (let [params (get-in request [:nav/route :params])]
    {:search/field (keyword (:field/entity params)
                            (:field/name params))
     :search/value (:field/value params)
     :user/id (get-in request [:auth/user :user/id])}))
