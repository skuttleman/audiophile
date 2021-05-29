(ns com.ben-allred.audiophile.api.handlers.validations.core
  (:require
    [clojure.spec.alpha :as s]
    [com.ben-allred.audiophile.api.handlers.validations.specs :as specs]
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]
    [spec-tools.core :as st]))

(defmacro ^:private spec-check! [spec data form]
  `(let [spec# ~spec
         data# ~data]
     (try
       ~form
       (catch Throwable _#
         (log/error "Invalid input" data# (s/explain-data spec# data#))
         (int/invalid-input!)))))

(defn ^:private data-responder [result]
  (if result
    [::http/ok {:data result}]
    [::http/not-found]))

(defn select-input
  "Selects relevant data from HTTP request to be passed to handler"
  [handler request]
  (specs/spec handler request))

(defmulti validate! (fn [spec _] spec))

(defmethod validate! :default [spec data]
  (spec-check! spec data (st/select-spec spec (st/conform! spec data))))

(defmethod validate! :api.ws/connect [spec data]
  (spec-check! spec data (st/conform! spec data)))

(defmethod ig/init-key ::with-spec [_ {:keys [->response handler spec]}]
  (let [->response (or ->response data-responder)]
    (fns/=>> (validate! spec) handler ->response)))

(defmethod ig/init-key ::ok [_ _]
  (partial into [::http/ok]))

(defmethod ig/init-key ::identity [_ _]
  identity)
