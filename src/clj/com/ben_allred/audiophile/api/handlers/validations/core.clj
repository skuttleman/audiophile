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

(defn validate!
  "validates requests and either returns a request with the conformed value assoc'ed
   at :valid/data or throws a bad-request response. route specs are defined by defining
   a method on [[com.ben-allred.audiophile.api.handlers.validations.specs/spec]]"
  [handler request]
  (if-let [[spec data] (specs/spec handler request)]
    (assoc request :valid/data (check! spec data "Invalid request"))
    request))
