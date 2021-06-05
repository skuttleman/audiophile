(ns com.ben-allred.audiophile.common.app.navigation.base
  (:require
    [bidi.bidi :as bidi]
    [clojure.set :as set]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.core.navigation.protocols :as pnav]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.core.stubs.pushy :as pushy]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.core.utils.uri :as uri]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn ^:private params->internal [params]
  (-> params
      (update :route-params (fns/=>
                              (maps/update-maybe :artifact-id uuids/->uuid)
                              (maps/update-maybe :file-id uuids/->uuid)
                              (maps/update-maybe :project-id uuids/->uuid)
                              (maps/update-maybe :team-id uuids/->uuid)))
      (update :query-params (fns/=>
                              (maps/update-maybe :file-version-id uuids/->uuid)))))

(defn ^:private internal->params [params]
  (-> params
      (update :route-params (fns/=>
                              (maps/update-maybe :artifact-id str)
                              (maps/update-maybe :file-id str)
                              (maps/update-maybe :project-id str)
                              (maps/update-maybe :team-id str)))
      (update :query-params (fns/=>
                              (maps/update-maybe :file-version-id str)))))

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

(defn ^:private on-nav [nav tracker pushy route]
  (when (get-in route [:query-params :error-msg])
    (->> (update route :query-params dissoc :error-msg)
         (serdes/serialize nav (:handle route))
         (pushy/replace-token! pushy)))
  (some-> tracker (pnav/on-change route)))

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

(defn nav [{:keys [router tracker]}]
  (let [pushy (volatile! nil)]
    (vreset! pushy (pushy/pushy #(on-nav router tracker @pushy %)
                                #(serdes/deserialize router %)))
    (doto (->LinkedNavigator @pushy router)
      pnav/start!)))

(defn nav#stop [nav]
  (pnav/stop! nav))
