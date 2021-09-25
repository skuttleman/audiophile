(ns com.ben-allred.audiophile.backend.infrastructure.http.ring
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [ring.middleware.cookies :as ring.cook]
    [ring.middleware.multipart-params :as ring.multi]
    [ring.middleware.resource :as ring.res]
    [ring.util.response :as ring.resp]))

(def ^{:arglists '([url])} redirect
  "Produce a ring-style response to redirect to a url"
  ring.resp/redirect)

(def ^{:arglists '([request root-path])} resource-request
  "Handler function that will respond with static assets when they exist in the resources folder"
  ring.res/resource-request)

(def ^{:arglists '([handler] [handler options])} wrap-cookies
  "Ring middleware for encoding and decoding cookies"
  ring.cook/wrap-cookies)

(def ^{:arglists '([handler] [handler options])} wrap-multipart-params
  ring.multi/wrap-multipart-params)

(defn ->cookie
  "Creates a ring-style cookie map suitable to be encoded by ring"
  ([k v]
   (->cookie k v nil))
  ([k v params]
   {k (maps/assoc-defaults params :value v :http-only true :path "/")}))

(defn error
  "Formats an error msg as an error response map"
  [status msg]
  [status {:error {:errors [{:message msg}]}}])
