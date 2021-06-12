(ns com.ben-allred.audiophile.ui.infrastructure.resources.base
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.ui.core.utils.reagent :as r]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.ben-allred.vow.impl.protocol :as pv]))

(defn ^:private ->watch-fn [resolve reject]
  (fn [watch-key state _ {:keys [status value error]}]
    (when (case status
            :success [(resolve value)]
            :error [(reject error)]
            nil)
      (remove-watch state watch-key))))

(deftype Resource [state opts->vow]
  pres/IResource
  (request! [_ opts]
    (swap! state assoc :status :requesting)
    (-> opts
        opts->vow
        (v/then :data (comp v/reject :errors))
        (v/peek (partial swap! state assoc :status :success :value)
                (partial swap! state assoc :status :error :error))))
  (status [_]
    (:status @state))

  pv/IPromise
  (then [_ on-success on-error]
    (v/then (v/create (fn [resolve reject]
                        (let [watch-key (gensym)
                              watcher (->watch-fn resolve reject)]
                          (add-watch state watch-key watcher)
                          (watcher watch-key state nil @state))))
            on-success
            on-error))

  IDeref
  (-deref [_]
    (let [{:keys [status value error]} @state]
      (case status
        :error error
        value))))

(defn base [{:keys [opts->vow]}]
  (->Resource (r/atom {:status :init}) opts->vow))

(defn http-handler [{:keys [http-client method opts->params opts->request route]}]
  (let [opts->params (or opts->params :nav/params)
        opts->request (or opts->request identity)]
    (fn [opts]
      (res/request! http-client
                    (maps/assoc-defaults (opts->request opts)
                                         :method method
                                         :nav/route route
                                         :nav/params (opts->params opts))))))

(defn file-uploader [_]
  (fn [{:keys [files] :as opts}]
    (assoc opts
           :multipart-params (map (partial conj ["files[]"]) files)
           :http/async? true)))
