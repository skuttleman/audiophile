(ns audiophile.common.infrastructure.navigation.base
  (:require
    #?(:cljs [audiophile.ui.store.actions :as act])
    [bidi.bidi :as bidi]
    [clojure.set :as set]
    [clojure.string :as string]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.protocols :as pserdes]
    [audiophile.common.core.stubs.pushy :as pushy]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uri :as uri]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.navigation.protocols :as pnav]
    [audiophile.common.infrastructure.navigation.routes :as routes]
    [audiophile.common.infrastructure.store.core :as store]))

(defn ^:private params->internal [params]
  (update params :params (fns/=>
                           (maps/update-maybe :artifact/id uuids/->uuid)
                           (maps/update-maybe :file/id uuids/->uuid)
                           (maps/update-maybe :project/id uuids/->uuid)
                           (maps/update-maybe :team/id uuids/->uuid)
                           (maps/update-maybe :file-version-id uuids/->uuid))))

(defn ^:private internal->params [params]
  (update params :params (fns/=>
                           (maps/update-maybe :artifact/id str)
                           (maps/update-maybe :file/id str)
                           (maps/update-maybe :project/id str)
                           (maps/update-maybe :team/id str)
                           (maps/update-maybe :file-version-id str))))

(defn ^:private serialize* [base-urls routes handle {:keys [params] :as opts}]
  (let [qp (uri/join-query (into {}
                                 (remove (comp namespace key))
                                 params))
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
                               :params)))
      base-url (->> (str base-url))
      (seq qp) (str "?" qp))))

(defn ^:private deserialize* [routes path]
  (let [[path' query-string] (string/split path #"\?")
        qp (uri/split-query query-string)]
    (some-> routes
            (bidi/match-route path')
            (assoc :path path')
            (set/rename-keys {:handler      :handle
                              :route-params :params})
            (cond->
              (seq qp) (update :params merge qp)
              query-string (assoc :query-string query-string))
            params->internal)))

(defn ^:private on-nav [nav store pushy route]
  (let [err-msg (get-in route [:params :error-msg])]
    #?(:cljs (store/dispatch! store [:router/update route]))
    (when err-msg
      (->> (update route :params dissoc :error-msg)
           (serdes/serialize nav (:handle route))
           (pushy/replace-token! pushy))
      #?(:cljs (store/dispatch! store (act/banner:add! :error (keyword err-msg)))))))

(deftype Router [base-urls routes]
  pnav/IHistory
  (navigate! [_ _])
  (replace! [_ _])

  pserdes/ISerde
  (serialize [_ handle opts]
    (serialize* base-urls routes handle opts))
  (deserialize [_ path _]
    (deserialize* routes path)))

(defn router
  "Constructor for creating [[Router]] used for controlling app navigation."
  [{:keys [base-urls]}]
  (->Router base-urls routes/all))

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

(defn nav
  "Constructor for [[LinkedNavigator]] which links the [[Router]] to the browser history API."
  [{:keys [router store]}]
  (let [pushy (volatile! nil)]
    (vreset! pushy (pushy/pushy #(on-nav router store @pushy %)
                                #(serdes/deserialize router %)))
    (doto (->LinkedNavigator @pushy router)
      pnav/start!)))

(defn nav#stop
  "Unlinks [[LinkedNavigator]] from the browser history API."
  [nav]
  (pnav/stop! nav))
