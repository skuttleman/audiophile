(ns com.ben-allred.audiophile.common.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.utils.query-params :as qp]))

(def ^:private routes
  [""
   [;; AUTH
    ["/auth"
     [["/callback" :auth/callback]
      ["/login" :auth/login]
      ["/logout" :auth/logout]
      [true :api/not-found]]]

    ;; API
    ["/api"
     [["/foo" :api/foo]
      [true :api/not-found]]]

    ;; UI
    ["/" :ui/home]

    ;; RESOURCES
    ["/health" :resources/health]
    ["/js" [[true :resources/js]]]
    ["/css" [[true :resources/css]]]
    [true :ui/not-found]]])

(defn match-route [path]
  (let [[path' query-string] (string/split path #"\?")
        qp (some-> query-string qp/decode)]
    (-> routes
        (bidi/match-route path)
        (assoc :path path')
        (cond->
          (seq qp) (assoc :query-params qp)
          query-string (assoc :query-string query-string)))))

(defn path-for
  ([handler]
   (path-for handler nil))
  ([handler {:keys [query-params route-params]}]
   (let [qp (some-> query-params qp/encode)]
     (-> (apply bidi/path-for routes handler (mapcat (fn [[k v]]
                                                       [k (str (cond-> v
                                                                 (keyword? v) name))])
                                                     route-params))
         (cond-> (seq qp) (str "?" qp))))))
