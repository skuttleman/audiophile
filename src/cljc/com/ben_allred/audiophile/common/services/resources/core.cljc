(ns com.ben-allred.audiophile.common.services.resources.core
  (:require
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.services.stubs.reagent :as r]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]
    [com.ben-allred.vow.impl.protocol :as pv]
    [integrant.core :as ig])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

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
    (let [{:keys [status value error]} @state]
      (v/then (v/create (fn [resolve reject]
                          (case status
                            :success (resolve value)
                            :error (reject error)
                            (let [watch-key (gensym)]
                              (add-watch state
                                         watch-key
                                         (fn [_ _ _ {:keys [status value error]}]
                                           (when (case status
                                                   :success [(resolve value)]
                                                   :error [(reject error)]
                                                   nil)
                                             (remove-watch state watch-key))))))))
              on-success
              on-error)))

  IDeref
  (#?(:cljs -deref :default deref) [_]
    (let [{:keys [status value error]} @state]
      (case status
        :error error
        value))))

(defmethod ig/init-key ::resource [_ {:keys [opts->vow]}]
  (->Resource (r/atom {:status :init}) opts->vow))

(defmethod ig/init-key ::http-handler [_ {:keys [http-client method opts->params opts->request route]}]
  (let [opts->params (or opts->params :nav/params)
        opts->request (or opts->request identity)]
    (fn [opts]
      (http/request! http-client
                     (maps/assoc-defaults (opts->request opts)
                                          :method method
                                          :nav/route route
                                          :nav/params (opts->params opts))))))

(defmethod ig/init-key ::file-uploader [_ _]
  (fn [{:keys [files] :as opts}]
    (assoc opts
           #?@(:clj  [:multipart (map (fn [file]
                                        {:part-name "files[]"
                                         :name      (.getName file)
                                         :content   file})
                                      files)]
               :cljs [:multipart-params (map (partial conj ["files[]"]) files)]))))

(defn request!
  ([resource]
   (request! resource nil))
  ([resource opts]
   (pres/request! resource opts)))

(defn status [resource]
  (pres/status resource))

(defn requested? [resource]
  (not= :init (status resource)))

(defn ready? [resource]
  (not= :requesting (status resource)))

(defn success? [resource]
  (= :success (status resource)))

(defn error? [resource]
  (= :error (status resource)))

(defn requesting? [resource]
  (= :requesting (status resource)))
