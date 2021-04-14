(ns com.ben-allred.audiophile.common.services.navigation
  (:require
    #?(:cljs [pushy.core :as pushy])
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.uri :as uri]
    [integrant.core :as ig]))

(defprotocol IHistory
  (-start! [this])
  (-stop! [this])
  (-navigate! [this path])
  (-replace! [this path]))

(defprotocol INavigate
  (-path-for [this page params])
  (match-route [this path]))

(deftype LinkedNavigator [pushy navigator]
  IHistory
  (-start! [_]
    #?(:cljs (pushy/start! pushy))
    nil)
  (-stop! [_]
    #?(:cljs (pushy/stop! pushy))
    nil)
  (-navigate! [_ path]
    #?(:cljs (pushy/set-token! pushy path))
    nil)
  (-replace! [_ path]
    #?(:cljs (pushy/replace-token! pushy path))
    nil)

  INavigate
  (-path-for [_ page params]
    (-path-for navigator page params))
  (match-route [_ path]
    (match-route navigator path)))

(deftype StubNavigator [routes]
  IHistory
  (-navigate! [_ _])
  (-replace! [_ _])

  INavigate
  (-path-for [_ page params]
    (let [{:keys [query-params route-params]} params
          qp (uri/join-query query-params)]
      (-> (apply bidi/path-for
                 routes
                 page
                 (mapcat (fn [[k v]]
                           [k (str (cond-> v
                                     (keyword? v) name))])
                         route-params))
          (cond-> (seq qp) (str "?" qp)))))
  (match-route [_ path]
    (let [[path' query-string] (string/split path #"\?")
          qp (uri/split-query query-string)]
      (-> routes
          (bidi/match-route path')
          (assoc :path path')
          (cond->
            (seq qp) (assoc :query-params qp)
            query-string (assoc :query-string query-string))))))

(defmethod ig/init-key ::stub [_ {:keys [routes]}]
  (->StubNavigator routes))

(defmethod ig/init-key ::nav [_ {:keys [routes store]}]
  (let [-nav (->StubNavigator routes)
        pushy #?(:cljs (pushy/pushy #(ui-store/dispatch! store [:router/updated %])
                                    (partial match-route -nav))
                 :default nil)]
    (doto (->LinkedNavigator pushy -nav)
      -start!)))

(defmethod ig/halt-key! ::nav [_ nav]
  (-stop! nav))

(defn path-for
  ([nav page]
   (path-for nav page nil))
  ([nav page params]
   (-path-for nav page params)))

(defn navigate!
  ([nav page]
   (navigate! nav page nil))
  ([nav page params]
   (-navigate! nav (-path-for nav page params))))

(defn replace!
  ([nav page]
   (replace! nav page nil))
  ([nav page params]
   (-replace! nav (-path-for nav page params))))
