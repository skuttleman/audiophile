(ns com.ben-allred.audiophile.api.domain.validations.core
  (:require
    [clojure.spec.alpha :as s]
    [com.ben-allred.audiophile.api.domain.validations.specs :as specs]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [spec-tools.core :as st]))

(defmacro ^:private spec-check! [spec data form]
  `(let [spec# ~spec
         data# ~data]
     (try
       ~form
       (catch Throwable ex#
         (log/error "Invalid input" data# (s/explain-data spec# data#))
         (throw ex#)))))

(defn select-input
  "Selects relevant data from HTTP request to be passed to handler"
  [handler request]
  (specs/spec handler request))

(defmulti validate! (fn [spec _] spec))

(defmethod validate! :default [spec data]
  (spec-check! spec data (st/select-spec spec (st/conform! spec data))))

(defmethod validate! :api.ws/connect [spec data]
  (spec-check! spec data (st/conform! spec data)))
