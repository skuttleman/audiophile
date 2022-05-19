(ns com.ben-allred.audiophile.ui.infrastructure.store.actions
  (:require
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.api.store.core :as store]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.vow.core :as v]))

(defmethod store/async* :user/load-profile! [_ {:keys [http-client nav store]}]
  (-> (http/get http-client (nav/path-for nav :api/profile))
      (v/then (fn [{profile :data}]
                (store/dispatch! store [:user/profile profile])))))
