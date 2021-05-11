(ns com.ben-allred.audiophile.api.handlers.validations.core
  (:require
    [com.ben-allred.audiophile.api.handlers.validations.specs :as specs]
    [com.ben-allred.audiophile.api.services.interactors.common :as int]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [spec-tools.core :as st]))

(defn validate!
  "validates requests and either returns a request with the conformed value assoc'ed
   at :valid/data or throws a bad-request response. route specs are defined by defining
   a method on [[com.ben-allred.audiophile.api.handlers.validations.specs/spec]]"
  [handler request]
  (try
    (let [[spec data] (specs/spec handler request)]
      (cond-> request
        spec (assoc :valid/data (st/select-spec spec (st/conform! spec data)))))
    (catch Throwable _
      (int/invalid-input!))))
