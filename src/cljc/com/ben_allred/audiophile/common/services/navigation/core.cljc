(ns com.ben-allred.audiophile.common.services.navigation.core
  (:require
    [bidi.bidi :as bidi]
    [clojure.set :as set]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.services.navigation.protocols :as pnav]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.services.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.services.stubs.dom :as dom]
    [com.ben-allred.audiophile.common.services.stubs.pushy :as pushy]
    [com.ben-allred.audiophile.common.services.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.fns :as fns]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.audiophile.common.utils.uri :as uri]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]))

(defn ^:private params->internal [params]
  (update params :route-params (fns/=>
                                 (maps/update-maybe :artifact-id uuids/->uuid)
                                 (maps/update-maybe :file-id uuids/->uuid)
                                 (maps/update-maybe :project-id uuids/->uuid)
                                 (maps/update-maybe :team-id uuids/->uuid))))

(defn ^:private internal->params [params]
  (update params :route-params (fns/=>
                                 (maps/update-maybe :artifact-id str)
                                 (maps/update-maybe :file-id str)
                                 (maps/update-maybe :project-id str)
                                 (maps/update-maybe :team-id str))))

(deftype LinkedNavigator [pushy router]
  pnav/IHistory
  (start! [_]
    (pushy/start! pushy)
    nil)
  (stop! [_]
    (pushy/stop! pushy)
    nil)
  (navigate! [_ path]
    (pushy/set-token! pushy path)
    nil)
  (replace! [_ path]
    (pushy/replace-token! pushy path)
    nil)

  pserdes/ISerde
  (serialize [_ handle opts]
    (pserdes/serialize router handle opts))
  (deserialize [_ path opts]
    (pserdes/deserialize router path opts)))

(defn ^:private serialize* [base-urls routes handle opts]
  (let [{:keys [query-params]} opts
        qp (uri/join-query query-params)
        base-url (get base-urls (keyword (namespace handle)))]
    (cond-> (apply bidi/path-for
                   routes
                   handle
                   (mapcat (fn [[k v]]
                             [k (str (cond-> v
                                       (keyword? v) name))])
                           (-> opts
                               (assoc :handle handle)
                               internal->params
                               :route-params)))
      base-url (->> (str base-url))
      (seq qp) (str "?" qp))))

(defn ^:private deserialize* [routes path]
  (let [[path' query-string] (string/split path #"\?")
        qp (uri/split-query query-string)]
    (some-> routes
            (bidi/match-route path')
            (assoc :path path')
            (set/rename-keys {:handler :handle})
            (cond->
              (seq qp) (assoc :query-params qp)
              query-string (assoc :query-string query-string))
            params->internal)))

(defn ^:private on-nav [-nav store pushy handle]
  (let [route (maps/update-maybe handle :query-params dissoc :error-msg)]
    (if-let [err (get-in handle [:query-params :error-msg])]
      (do (ui-store/dispatch! store (actions/server-err! err))
          (pushy/replace-token! pushy (serdes/serialize -nav (:handle route) route)))
      (ui-store/dispatch! store [:router/updated route]))))

(deftype Router [base-urls routes]
  pnav/IHistory
  (navigate! [_ _])
  (replace! [_ _])

  pserdes/ISerde
  (serialize [_ handle opts]
    (serialize* base-urls routes handle opts))
  (deserialize [_ path _]
    (deserialize* routes path)))

(defn router [{:keys [base-urls routes]}]
  (->Router base-urls routes))

(defn nav [{:keys [router store]}]
  (let [pushy (volatile! nil)]
    (vreset! pushy (pushy/pushy #(on-nav router store @pushy %)
                                #(serdes/deserialize router %)))
    (doto (->LinkedNavigator @pushy router)
      pnav/start!)))

(defn nav#stop [nav]
  (pnav/stop! nav))

(defn path-for
  ([nav handle]
   (path-for nav handle nil))
  ([nav handle params]
   (serdes/serialize nav handle params)))

(defn navigate!
  "push a path + params to the browser's history"
  ([nav handle]
   (navigate! nav handle nil))
  ([nav handle params]
   (pnav/navigate! nav (path-for nav handle params))))

(defn replace!
  ([nav handle]
   (replace! nav handle nil))
  ([nav handle params]
   (pnav/replace! nav (path-for nav handle params))))

(defn goto!
  "send the browser to a location with a page rebuild"
  ([nav handle]
   (goto! nav handle nil))
  ([nav handle params]
   (dom/assign! (path-for nav handle params))))

(defn match-route [nav path]
  (serdes/deserialize nav path))
