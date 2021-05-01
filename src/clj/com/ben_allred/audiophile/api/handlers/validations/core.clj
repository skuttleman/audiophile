(ns com.ben-allred.audiophile.api.handlers.validations.core
  (:require
    [com.ben-allred.audiophile.api.handlers.validations.specs :as specs]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [spec-tools.core :as st]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defn ^:private check! [spec handler value msg]
  (try (st/select-spec spec (st/conform! spec value))
       (catch Throwable ex
         (log/spy :debug ["spec input error" handler value])
         (ring/abort! msg ::http/bad-request ex))))

(defn validate!
  "validates requests and either returns a request with conformed assoc'ed at :valid/data
   or throws a bad-request response. route specs are defined by defining a method on
   [[com.ben-allred.audiophile.api.handlers.validations.specs/spec]]"
  [handler {{:keys [data]} :body :as request}]
  (assoc request :valid/data (if-let [spec (specs/spec handler)]
                               (check! spec handler data "invalid request")
                               data)))
