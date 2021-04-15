(ns com.ben-allred.audiophile.common.services.navigation.core
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.services.stubs.dom :as dom]
    [com.ben-allred.audiophile.common.services.stubs.pushy :as pushy]
    [com.ben-allred.audiophile.common.services.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.audiophile.common.utils.uri :as uri]
    [integrant.core :as ig]))

(defmulti params->internal :handler)
(defmethod params->internal :default
  [params]
  params)

(defmulti internal->params :handler)
(defmethod internal->params :default
  [params]
  params)

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
    (pushy/start! pushy)
    nil)
  (-stop! [_]
    (pushy/stop! pushy)
    nil)
  (-navigate! [_ path]
    (pushy/set-token! pushy path)
    nil)
  (-replace! [_ path]
    (pushy/replace-token! pushy path)
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
    (let [{:keys [query-params]} params
          qp (uri/join-query query-params)]
      (-> (apply bidi/path-for
                 routes
                 page
                 (mapcat (fn [[k v]]
                           [k (str (cond-> v
                                     (keyword? v) name))])
                         (-> params
                             (assoc :handler page)
                             internal->params
                             :route-params)))
          (cond-> (seq qp) (str "?" qp)))))
  (match-route [_ path]
    (let [[path' query-string] (string/split path #"\?")
          qp (uri/split-query query-string)]
      (-> routes
          (bidi/match-route path')
          params->internal
          (assoc :path path')
          (cond->
            (seq qp) (assoc :query-params qp)
            query-string (assoc :query-string query-string))))))

(defmethod ig/init-key ::stub [_ {:keys [routes]}]
  (->StubNavigator routes))

(defn ^:private on-nav [-nav store pushy page]
  (let [page' (maps/update-maybe page :query-params dissoc :error-msg)]
    (if-let [err (get-in page [:query-params :error-msg])]
      (do (ui-store/dispatch! store (actions/server-err! err))
          (pushy/replace-token! pushy (-path-for -nav (:handler page') page')))
      (ui-store/dispatch! store [:router/updated page']))))

(defmethod ig/init-key ::nav [_ {:keys [routes store]}]
  (let [-nav (->StubNavigator routes)
        pushy (volatile! nil)]
    (vreset! pushy (pushy/pushy #(on-nav -nav store @pushy %)
                                (partial match-route -nav)))
    (doto (->LinkedNavigator @pushy -nav)
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

(defn goto!
  ([nav page]
   (goto! nav page nil))
  ([nav page params]
   (dom/assign! (path-for nav page params))))
