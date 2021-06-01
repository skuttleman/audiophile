(ns com.ben-allred.audiophile.common.app.http
  (:refer-clojure :exclude [get])
  (:require
    [clojure.set :as set]
    [com.ben-allred.audiophile.common.app.resources.protocols :as pres]))

(def status->code
  {::ok 200
   ::created 201
   ::accepted 202
   ::non-authoritative-information 203
   ::no-content 204
   ::reset-content 205
   ::partial-content 206
   ::multiple-choices 300
   ::moved-permanently 301
   ::found 302
   ::see-other 303
   ::not-modified 304
   ::use-proxy 305
   ::unused 306
   ::temporary-redirect 307
   ::bad-request 400
   ::unauthorized 401
   ::payment-required 402
   ::forbidden 403
   ::not-found 404
   ::method-not-allowed 405
   ::not-acceptable 406
   ::proxy-authentication-required 407
   ::request-timeout 408
   ::conflict 409
   ::gone 410
   ::length-required 411
   ::precondition-failed 412
   ::request-entity-too-large 413
   ::request-uri-too-long 414
   ::unsupported-media-type 415
   ::requested-range-not-satisfiable 416
   ::expectation-failed 417
   ::internal-server-error 500
   ::not-implemented 501
   ::bad-gateway 502
   ::service-unavailable 503
   ::gateway-timeout 504
   ::http-version-not-supported 505})

(def code->status
  (set/map-invert status->code))

(defn ^:private check-status [lower upper response]
  (let [status (or (when (vector? response)
                     (status->code (first response)))
                   (as-> response $
                         (cond-> $ (vector? $) second)
                         (:status $)
                         (cond-> $ (keyword? $) status->code)))]
    (<= lower status upper)))

(def ^{:arglists '([response])} success?
  (partial check-status 200 299))

(def ^{:arglists '([response])} redirect?
  (partial check-status 300 399))

(def ^{:arglists '([response])} client-error?
  (partial check-status 400 499))

(def ^{:arglists '([response])} server-error?
  (partial check-status 500 599))

(def ^{:arglists '([response])} error?
  (some-fn client-error? server-error?))

(defn request! [client request]
  (pres/request! client request))

(defn ^:private http* [client method url request]
  (request! client (assoc request :url url :method method)))

(defn get
  ([client url]
   (get client url nil))
  ([client url request]
   (http* client :get url request)))

(defn post [client url request]
  (http* client :post url request))

(defn patch [client url request]
  (http* client :patch url request))

(defn put [client url request]
  (http* client :put url request))

(defn delete
  ([client url]
   (delete client url nil))
  ([client url request]
   (http* client :delete url request)))

