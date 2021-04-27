(ns com.ben-allred.audiophile.common.services.navigation.core
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.services.serdes.protocols :as pserdes]
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
  "This can only be implemented in browser targeted cljs builds"
  (-start! [this] "start monitoring and reacting to the browser history state")
  (-stop! [this] "stop interacting with the browser history state")
  (-navigate! [this path] "push a new state to the browser history")
  (-replace! [this path] "replace the current browser history state"))

(deftype LinkedNavigator [pushy router]
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

  pserdes/ISerde
  (serialize [_ page opts]
    (pserdes/serialize router page opts))
  (deserialize [_ path opts]
    (pserdes/deserialize router path opts)))

(defn serialize* [routes page opts]
  (let [{:keys [query-params]} opts
        qp (uri/join-query query-params)]
    (-> (apply bidi/path-for
               routes
               page
               (mapcat (fn [[k v]]
                         [k (str (cond-> v
                                   (keyword? v) name))])
                       (-> opts
                           (assoc :handler page)
                           internal->params
                           :route-params)))
        (cond-> (seq qp) (str "?" qp)))))

(defn deserialize* [routes path]
  (let [[path' query-string] (string/split path #"\?")
        qp (uri/split-query query-string)]
    (some-> routes
            (bidi/match-route path')
            (assoc :path path')
            (cond->
              (seq qp) (assoc :query-params qp)
              query-string (assoc :query-string query-string))
            params->internal)))

(deftype Router [routes]
  IHistory
  (-navigate! [_ _])
  (-replace! [_ _])

  pserdes/ISerde
  (serialize [_ page opts]
    (serialize* routes page opts))
  (deserialize [_ path _]
    (deserialize* routes path)))

(defmethod ig/init-key ::router [_ {:keys [routes]}]
  (->Router routes))

(defn ^:private on-nav [-nav store pushy page]
  (let [page' (maps/update-maybe page :query-params dissoc :error-msg)]
    (if-let [err (get-in page [:query-params :error-msg])]
      (do (ui-store/dispatch! store (actions/server-err! err))
          (pushy/replace-token! pushy (serdes/serialize -nav (:handler page') page')))
      (ui-store/dispatch! store [:router/updated page']))))

(defmethod ig/init-key ::nav [_ {:keys [routes store]}]
  (let [router (->Router routes)
        pushy (volatile! nil)]
    (vreset! pushy (pushy/pushy #(on-nav router store @pushy %)
                                #(serdes/deserialize router %)))
    (doto (->LinkedNavigator @pushy router)
      -start!)))

(defmethod ig/halt-key! ::nav [_ nav]
  (-stop! nav))

(defn path-for
  ([nav page]
   (path-for nav page nil))
  ([nav page params]
   (serdes/serialize nav page params)))

(defn navigate!
  "push a path + params to the browser's history"
  ([nav page]
   (navigate! nav page nil))
  ([nav page params]
   (-navigate! nav (path-for nav page params))))

(defn replace!
  ([nav page]
   (replace! nav page nil))
  ([nav page params]
   (-replace! nav (path-for nav page params))))

(defn goto!
  "send the browser to a location with a page rebuild"
  ([nav page]
   (goto! nav page nil))
  ([nav page params]
   (dom/assign! (path-for nav page params))))

(defn match-route [nav path]
  (serdes/deserialize nav path))
