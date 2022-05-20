(ns com.ben-allred.audiophile.ui.infrastructure.store.async
  (:require
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.common.infrastructure.navigation.core :as nav]
    [com.ben-allred.audiophile.common.infrastructure.store.core :as store]
    [com.ben-allred.audiophile.ui.infrastructure.store.actions :as act]
    [com.ben-allred.vow.core :as v :include-macros true]))

(defmethod store/async* :user.profile/load! [_ {:keys [http-client nav store]}]
  (-> (http/get http-client (nav/path-for nav :api/profile))
      (v/then-> :data act/profile:set! (->> (store/dispatch! store)))))
