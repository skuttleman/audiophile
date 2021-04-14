(ns com.ben-allred.audiophile.common.services.resources.core
  (:require
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.stubs :as st]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]
    [integrant.core :as ig])
  #?(:clj (:import
            (clojure.lang IDeref))))

(deftype Resource [id state dispatch! opts->vow]
  pres/IResource
  (request! [_ opts]
    (dispatch! [:resource/request id])
    (-> opts
        opts->vow
        (v/then :data (comp v/reject :errors))
        (v/peek #(dispatch! [:resource/success id %])
                #(dispatch! [:resource/failure id %]))))

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
        :error [status error]
        [status value]))))

(defmethod ig/init-key ::resource [_ {:keys [store opts->vow]}]
  (let [dispatch! (partial ui-store/dispatch! store)
        state (st/atom nil)
        id (gensym "RESOURCE__")]
    (dispatch! [:resource/register id (partial reset! state)])
    (->Resource id state dispatch! opts->vow)))

(defn request!
  ([resource]
   (request! resource nil))
  ([resource opts]
   (pres/request! resource opts)))
