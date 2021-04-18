(ns com.ben-allred.audiophile.common.services.resources.core
  (:require
    [com.ben-allred.audiophile.common.services.http :as http]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.services.stubs.reagent :as r]
    [com.ben-allred.vow.core :as v]
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
      (case status
        :success (on-success value)
        :error (on-error error)
        (let [watch-key (gensym)]
          (add-watch state watch-key (fn [_ _ _ {:keys [status value error]}]
                                       (when (case status
                                               :success [(on-success value)]
                                               :error [(on-error error)]
                                               nil)
                                         (remove-watch state watch-key))))))))

  IDeref
  (#?(:cljs -deref :default deref) [_]
    (let [{:keys [status value error]} @state]
      (case status
        :error error
        value))))

(defmethod ig/init-key ::resource [_ {:keys [opts->vow]}]
  (let [state (r/atom {:status :init})]
    (->Resource state opts->vow)))

(defmethod ig/init-key ::http-handler [_ {:keys [http-client method nav opts->params opts->request route]}]
  (let [opts->params (or opts->params (constantly nil))
        opts->request (or opts->request identity)]
    (fn [opts]
      (http/go http-client
               method
               (nav/path-for nav route (opts->params opts))
               (opts->request opts)))))

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
