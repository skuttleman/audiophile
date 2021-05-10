(ns com.ben-allred.audiophile.api.handlers.validations.core
  (:require
    [com.ben-allred.audiophile.api.handlers.validations.specs :as specs]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [spec-tools.core :as st]))

(defn ^:private check! [spec data msg]
  (try (st/select-spec spec (st/conform! spec data))
       (catch Throwable ex
         (ring/abort! msg ::http/bad-request ex))))

(defn ^:private validate-with-spec! [request [spec data]]
  (cond-> request
    spec (assoc :valid/data (check! spec data "Invalid request"))))

(defn validate!
  "validates requests and either returns a request with the conformed value assoc'ed
   at :valid/data or throws a bad-request response. route specs are defined by defining
   a method on [[com.ben-allred.audiophile.api.handlers.validations.specs/spec]]"
  [handler request]
  (validate-with-spec! request (specs/spec handler request)))
