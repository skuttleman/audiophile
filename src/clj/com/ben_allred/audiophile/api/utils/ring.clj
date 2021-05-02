(ns com.ben-allred.audiophile.api.utils.ring
  (:require
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.audiophile.common.utils.http :as http]
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

(defn decode-cookies
  "Test utility for decoding response cookies"
  [response]
  (->> (get-in response [:headers "Set-Cookie"])
       (map #(:cookies (ring.cook/cookies-request {:headers {"cookie" %}})))
       (reduce merge {})))

(defn ->cookie
  "Creates a ring-style cookie map suitable to be encoded by ring"
  ([k v]
   (->cookie k v nil))
  ([k v params]
   {k (maps/assoc-defaults params :value v :http-only true :path "/")}))

(defn abort!
  "Throws an exception that contains response data that can be caught by route middleware."
  ([msg]
   (abort! msg ::http/bad-request))
  ([msg status]
   (abort! msg status nil))
  ([msg status cause]
   (throw (ex-info msg {:response [status {:errors [{:message msg}]}]} cause))))
