(ns com.ben-allred.audiophile.api.handlers.validations.core
  (:require
    [com.ben-allred.audiophile.api.handlers.validations.specs :as specs]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [spec-tools.core :as st]))

(defn ^:private check!
  ([spec value msg]
   (check! spec value msg ::http/bad-request))
  ([spec value msg status]
   (try (st/select-spec spec (st/conform! spec value))
        (catch Throwable ex
          (ring/abort! msg status ex)))))

(defn validate! [handler {:keys [body] :as request}]
  (assoc request :validations/body (if-let [spec (specs/spec handler)]
                                     (check! spec body "invalid request")
                                     body)))
