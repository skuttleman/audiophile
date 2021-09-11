(ns com.ben-allred.audiophile.backend.api.validations.selectors)

(defmulti select
          "extracts input data from"
          (fn [handler _] handler))

(defmethod select :default
  [_ request]
  request)

(defmethod select [:get :ws/connection]
  [_ request]
  (-> request
      (assoc :user/id (get-in request [:auth/user :user/id]))
      (merge (:headers request) (get-in request [:nav/route :query-params]))))
