(ns audiophile.backend.api.validations.selectors)

(defmulti select
          "extracts input data from"
          (fn [handler _] handler))

(defmethod select :default
  [_ request]
  request)
