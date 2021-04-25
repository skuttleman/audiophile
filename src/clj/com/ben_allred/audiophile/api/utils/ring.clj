(ns com.ben-allred.audiophile.api.utils.ring
  (:require
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [ring.middleware.cookies :as ring.cook]
    [ring.middleware.resource :as ring.res]
    [ring.util.response :as ring.resp]))

(def ^{:arglists '([url])} redirect
  "produce a ring-style response to redirect to a url"
  ring.resp/redirect)

(def ^{:arglists '([request root-path])} resource-request
  "handler function that will respond with static assets when they exist in the resources folder"
  ring.res/resource-request)

(def ^{:arglists '([handler] [handler options])} wrap-cookies
  "ring middleware for encoding and decoding cookies"
  ring.cook/wrap-cookies)

(defn decode-cookies
  "test utility for decoding response cookies"
  [response]
  (->> (get-in response [:headers "Set-Cookie"])
       (map #(:cookies (ring.cook/cookies-request {:headers {"cookie" %}})))
       (reduce merge {})))

(defn ->cookie
  "creates a ring-style cookie map suitable to be encoded by ring"
  ([k v]
   (->cookie k v nil))
  ([k v params]
   {k (maps/assoc-defaults params :value v :http-only true :path "/")}))

(defn abort!
  ([msg]
   (abort! msg ::http/bad-request))
  ([msg status]
   (abort! msg status nil))
  ([msg status cause]
   (throw (ex-info msg {:response [status {:errors [{:message msg}]}]} cause))))
